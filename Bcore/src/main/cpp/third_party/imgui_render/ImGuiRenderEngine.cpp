#include "ImGuiRenderEngine.h"
#include "ImGuiMenuUI.h"
#include "IL2CPPTouchHook.h"
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/input.h>
#include <android/native_window.h>
#include <android/log.h>
#include <mutex>
#include <atomic>
#include <cctype>
#include <cstdlib>
#include <cstring>
#include <thread>
#include <chrono>
#include <set>
#include <vector>
#include <string>
#include <fstream>
#include "shadowhook.h"
#include "imgui.h"
#include "backends/imgui_impl_opengl3.h"
#include "backends/imgui_impl_android.h"

#define LOG_TAG "ImGuiRenderEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::atomic<ImGuiState> g_ImGuiState(ImGuiState::Disabled);
static std::atomic<GuestRenderState> g_GuestState(GuestRenderState::NoGuest);
static std::atomic<bool> g_RenderEnabled(false);
static std::atomic<bool> g_HooksInstalled(false);

static bool g_EngineInitialized = false;
static std::string g_TargetAppPackage = "";
static std::mutex g_EngineMutex;

static std::set<std::string> g_HookedLibraries;
static std::mutex g_HookedMutex;

static EGLContext g_CurrentContext = EGL_NO_CONTEXT;
static int g_FrameCounter = 0;

static std::string GetCurrentProcessName() {
    std::string processName;
    std::ifstream cmdline("/proc/self/cmdline");
    if (cmdline.is_open()) {
        std::getline(cmdline, processName, '\0');
        cmdline.close();
    }
    return processName;
}

bool ImGuiRenderEngine::isVirtualGuestProcess() {
    static int s_IsGuestCached = -1; // -1: uncalculated, 0: false, 1: true
    if (s_IsGuestCached != -1) {
        return s_IsGuestCached == 1;
    }

    std::string processName = GetCurrentProcessName();
    if (processName.empty()) {
        s_IsGuestCached = 0;
        return false;
    }

    size_t colonPos = processName.find(':');
    if (colonPos == std::string::npos) {
        // Main host process (e.g., com.equinox.virtual) -> NOT guest
        s_IsGuestCached = 0;
        return false;
    }

    std::string suffix = processName.substr(colonPos + 1);
    if (suffix.length() >= 2 && suffix[0] == 'p') {
        bool allDigits = true;
        for (size_t i = 1; i < suffix.length(); ++i) {
            if (!isdigit(suffix[i])) {
                allDigits = false;
                break;
            }
        }
        if (allDigits) {
            int processIndex = std::atoi(suffix.c_str() + 1);
            if (processIndex >= 0 && processIndex <= 49) {
                s_IsGuestCached = 1;
                return true;
            }
        }
    }

    // Other process suffixes (e.g. :daemon, :server) -> NOT guest
    s_IsGuestCached = 0;
    return false;
}

static EGLBoolean hook_eglSwapBuffers(EGLDisplay dpy, EGLSurface surface);
static EGLBoolean hook_eglSwapBuffersWithDamageEXT(EGLDisplay dpy, EGLSurface surface, EGLint *rects, EGLint n_rects);
static EGLBoolean hook_eglSwapBuffersWithDamageKHR(EGLDisplay dpy, EGLSurface surface, EGLint *rects, EGLint n_rects);

static void* hook_eglGetProcAddress(const char* procname) {
    SHADOWHOOK_STACK_SCOPE();

    if (procname != nullptr) {
        if (strcmp(procname, "eglSwapBuffers") == 0) {
            LOGD("eglGetProcAddress requested eglSwapBuffers -> redirecting to our hook");
            return (void*)hook_eglSwapBuffers;
        } else if (strcmp(procname, "eglSwapBuffersWithDamageEXT") == 0) {
            LOGD("eglGetProcAddress requested eglSwapBuffersWithDamageEXT -> redirecting to our hook");
            return (void*)hook_eglSwapBuffersWithDamageEXT;
        } else if (strcmp(procname, "eglSwapBuffersWithDamageKHR") == 0) {
            LOGD("eglGetProcAddress requested eglSwapBuffersWithDamageKHR -> redirecting to our hook");
            return (void*)hook_eglSwapBuffersWithDamageKHR;
        }
    }

    return SHADOWHOOK_CALL_PREV(hook_eglGetProcAddress, procname);
}

static void* hook_dlsym(void* handle, const char* symbol) {
    SHADOWHOOK_STACK_SCOPE();

    if (symbol != nullptr) {
        if (strcmp(symbol, "eglSwapBuffers") == 0) {
            LOGD("dlsym requested eglSwapBuffers -> redirecting to our hook");
            return (void*)hook_eglSwapBuffers;
        } else if (strcmp(symbol, "eglSwapBuffersWithDamageEXT") == 0) {
            LOGD("dlsym requested eglSwapBuffersWithDamageEXT -> redirecting to our hook");
            return (void*)hook_eglSwapBuffersWithDamageEXT;
        } else if (strcmp(symbol, "eglSwapBuffersWithDamageKHR") == 0) {
            LOGD("dlsym requested eglSwapBuffersWithDamageKHR -> redirecting to our hook");
            return (void*)hook_eglSwapBuffersWithDamageKHR;
        }
    }

    return SHADOWHOOK_CALL_PREV(hook_dlsym, handle, symbol);
}

static bool is_library_loaded(const std::string& lib_name);

static void render_imgui_frame(EGLDisplay dpy, EGLSurface surface) {
    if (!ImGuiRenderEngine::isVirtualGuestProcess()) {
        return;
    }

    if (!g_RenderEnabled.load() || g_GuestState.load() != GuestRenderState::GuestRunning) {
        return;
    }

    EGLContext ctx = eglGetCurrentContext();
    if (ctx == EGL_NO_CONTEXT) {
        return;
    }

    std::lock_guard<std::mutex> lock(g_EngineMutex);

    g_FrameCounter++;
    
    // 1. Warm-up delay: Wait 150 frames to ensure engine's own rendering and EGL initialization is stable.
    if (g_FrameCounter < 150) {
        return;
    }

    // 2. Engine-specific delay: If libil2cpp is loaded, wait until the engine is fully initialized (indicated by touch hooks being active).
    if (is_library_loaded("libil2cpp.so") && !IL2CPPTouchHook::IsInstalled()) {
        return;
    }

    if (g_FrameCounter % 300 == 0) {
        LOGD("render_imgui_frame is ALIVE on context %p", ctx);
    }

    if (g_CurrentContext != ctx) {
        LOGD("EGL Context changed from %p to %p - Re-initializing ImGui backend", g_CurrentContext, ctx);
        if (g_EngineInitialized) {
            g_ImGuiState.store(ImGuiState::ContextLost);
            ImGui_ImplOpenGL3_Shutdown();
            g_EngineInitialized = false;
        }
        g_CurrentContext = ctx;
        g_ImGuiState.store(ImGuiState::WaitingForSurface);
    }

    EGLint width = 0, height = 0;
    eglQuerySurface(dpy, surface, EGL_WIDTH, &width);
    eglQuerySurface(dpy, surface, EGL_HEIGHT, &height);

    if (width > 0 && height > 0) {
        if (!g_EngineInitialized) {
            g_ImGuiState.store(ImGuiState::Initializing);
            IMGUI_CHECKVERSION();
            if (ImGui::GetCurrentContext() == nullptr) {
                ImGui::CreateContext();
                ImGuiIO& io = ImGui::GetIO();
                io.IniFilename = nullptr;

                ImGui::StyleColorsDark();
                ImGuiStyle& style = ImGui::GetStyle();
                style.WindowRounding = 10.0f;
                style.FrameRounding = 6.0f;
                style.PopupRounding = 6.0f;
                style.ScrollbarRounding = 6.0f;
                style.GrabRounding = 4.0f;
                style.WindowBorderSize = 1.0f;
                style.ScaleAllSizes(1.4f);
            }

            const char* gl_version_str = (const char*)glGetString(GL_VERSION);
            const char* gles_version = "#version 300 es";
            if (gl_version_str != nullptr && strstr(gl_version_str, "OpenGL ES 2.0") != nullptr) {
                gles_version = "#version 100";
            }
            LOGD("Initializing ImGui for Context %p. Detected OpenGL ES version: %s -> Using shader version: %s", 
                 ctx, gl_version_str ? gl_version_str : "Unknown", gles_version);

            if (ImGui_ImplOpenGL3_Init(gles_version)) {
                g_EngineInitialized = true;
                g_ImGuiState.store(ImGuiState::Ready);
                LOGD("ImGui Render Engine initialized successfully (%dx%d) on context %p", width, height, ctx);
            } else {
                g_ImGuiState.store(ImGuiState::Failed);
                LOGE("ImGui_ImplOpenGL3_Init failed in Render Engine on context %p", ctx);
                return;
            }
        }

        if (g_EngineInitialized && g_ImGuiState.load() == ImGuiState::Ready) {
            GLint last_program = 0;
            GLint last_active_texture = 0;
            GLint last_texture = 0;
            GLint last_array_buffer = 0;
            GLint last_element_array_buffer = 0;
            GLint last_vertex_array = 0;
            GLint last_viewport[4] = {0};
            GLint last_scissor_box[4] = {0};
            GLint last_blend_src_rgb = 0, last_blend_dst_rgb = 0;
            GLint last_blend_src_alpha = 0, last_blend_dst_alpha = 0;
            GLint last_blend_equation_rgb = 0, last_blend_equation_alpha = 0;
            GLboolean last_enable_blend = GL_FALSE;
            GLboolean last_enable_cull_face = GL_FALSE;
            GLboolean last_enable_depth_test = GL_FALSE;
            GLboolean last_enable_scissor_test = GL_FALSE;

            glGetIntegerv(GL_CURRENT_PROGRAM, &last_program);
            glGetIntegerv(GL_ACTIVE_TEXTURE, &last_active_texture);
            glGetIntegerv(GL_TEXTURE_BINDING_2D, &last_texture);
            glGetIntegerv(GL_ARRAY_BUFFER_BINDING, &last_array_buffer);
            glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, &last_element_array_buffer);
            glGetIntegerv(GL_VERTEX_ARRAY_BINDING, &last_vertex_array);
            glGetIntegerv(GL_VIEWPORT, last_viewport);
            glGetIntegerv(GL_SCISSOR_BOX, last_scissor_box);
            glGetIntegerv(GL_BLEND_SRC_RGB, &last_blend_src_rgb);
            glGetIntegerv(GL_BLEND_DST_RGB, &last_blend_dst_rgb);
            glGetIntegerv(GL_BLEND_SRC_ALPHA, &last_blend_src_alpha);
            glGetIntegerv(GL_BLEND_DST_ALPHA, &last_blend_dst_alpha);
            glGetIntegerv(GL_BLEND_EQUATION_RGB, &last_blend_equation_rgb);
            glGetIntegerv(GL_BLEND_EQUATION_ALPHA, &last_blend_equation_alpha);
            last_enable_blend = glIsEnabled(GL_BLEND);
            last_enable_cull_face = glIsEnabled(GL_CULL_FACE);
            last_enable_depth_test = glIsEnabled(GL_DEPTH_TEST);
            last_enable_scissor_test = glIsEnabled(GL_SCISSOR_TEST);

            ImGuiIO& io = ImGui::GetIO();
            io.DisplaySize = ImVec2((float)width, (float)height);

            ImGui_ImplOpenGL3_NewFrame();
            ImGui::NewFrame();

            ImGuiRenderEngine::renderOverlay(width, height);

            ImGui::Render();
            glViewport(0, 0, width, height);
            ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());

            glUseProgram(last_program);
            glActiveTexture(last_active_texture);
            glBindTexture(GL_TEXTURE_2D, last_texture);
            glBindVertexArray(last_vertex_array);
            glBindBuffer(GL_ARRAY_BUFFER, last_array_buffer);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, last_element_array_buffer);
            glBlendFuncSeparate(last_blend_src_rgb, last_blend_dst_rgb, last_blend_src_alpha, last_blend_dst_alpha);
            glBlendEquationSeparate(last_blend_equation_rgb, last_blend_equation_alpha);
            glViewport(last_viewport[0], last_viewport[1], last_viewport[2], last_viewport[3]);
            glScissor(last_scissor_box[0], last_scissor_box[1], last_scissor_box[2], last_scissor_box[3]);
            
            if (last_enable_blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
            if (last_enable_cull_face) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
            if (last_enable_depth_test) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
            if (last_enable_scissor_test) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
        }
    }
}

static EGLBoolean hook_eglSwapBuffers(EGLDisplay dpy, EGLSurface surface) {
    SHADOWHOOK_STACK_SCOPE();
    render_imgui_frame(dpy, surface);
    return SHADOWHOOK_CALL_PREV(hook_eglSwapBuffers, dpy, surface);
}

static EGLBoolean hook_eglSwapBuffersWithDamageEXT(EGLDisplay dpy, EGLSurface surface, EGLint *rects, EGLint n_rects) {
    SHADOWHOOK_STACK_SCOPE();
    render_imgui_frame(dpy, surface);
    return SHADOWHOOK_CALL_PREV(hook_eglSwapBuffersWithDamageEXT, dpy, surface, rects, n_rects);
}

static EGLBoolean hook_eglSwapBuffersWithDamageKHR(EGLDisplay dpy, EGLSurface surface, EGLint *rects, EGLint n_rects) {
    SHADOWHOOK_STACK_SCOPE();
    render_imgui_frame(dpy, surface);
    return SHADOWHOOK_CALL_PREV(hook_eglSwapBuffersWithDamageKHR, dpy, surface, rects, n_rects);
}

static int32_t hook_AInputQueue_getEvent(void* queue, AInputEvent** outEvent) {
    SHADOWHOOK_STACK_SCOPE();

    while (true) {
        int32_t res = SHADOWHOOK_CALL_PREV(hook_AInputQueue_getEvent, queue, outEvent);
        if (res != 0 || outEvent == nullptr || *outEvent == nullptr) {
            return res;
        }

        AInputEvent* event = *outEvent;
        bool swallow = false;

        if (ImGuiRenderEngine::isVirtualGuestProcess() && g_EngineInitialized && g_RenderEnabled.load() && g_ImGuiState.load() == ImGuiState::Ready) {
            int32_t type = AInputEvent_getType(event);
            if (type == AINPUT_EVENT_TYPE_MOTION) {
                int32_t action = AMotionEvent_getAction(event) & AMOTION_EVENT_ACTION_MASK;
                float x = AMotionEvent_getX(event, 0);
                float y = AMotionEvent_getY(event, 0);

                ImGuiIO& io = ImGui::GetIO();
                io.AddMousePosEvent(x, y);

                if (action == AMOTION_EVENT_ACTION_DOWN) {
                    io.AddMouseButtonEvent(0, true);
                } else if (action == AMOTION_EVENT_ACTION_UP) {
                    io.AddMouseButtonEvent(0, false);
                }

                if (io.WantCaptureMouse) {
                    swallow = true;
                }
            } else {
                if (ImGui_ImplAndroid_HandleInputEvent(event)) {
                    swallow = true;
                }
            }
        }

        if (swallow) {
            AInputQueue_finishEvent(static_cast<AInputQueue*>(queue), event, 1);
            continue;
        }

        return res;
    }
}

void ImGuiRenderEngine::renderOverlay(int width, int height) {
    ImGuiMenuUI::renderOverlay(width, height, g_TargetAppPackage);
}

static bool is_library_loaded(const std::string& lib_name) {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return false;

    char line[512];
    bool loaded = false;
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, lib_name.c_str()) != nullptr) {
            loaded = true;
            break;
        }
    }
    fclose(fp);
    return loaded;
}

static void apply_hooks_to_library(const std::string& lib_name) {
    LOGD("Manually applying ShadowHook PLT Hooks to loaded library: %s", lib_name.c_str());

    void* h1 = shadowhook_hook_sym_name(lib_name.c_str(), "eglSwapBuffers", (void*)hook_eglSwapBuffers, nullptr);
    void* h2 = shadowhook_hook_sym_name(lib_name.c_str(), "eglSwapBuffersWithDamageEXT", (void*)hook_eglSwapBuffersWithDamageEXT, nullptr);
    void* h3 = shadowhook_hook_sym_name(lib_name.c_str(), "eglSwapBuffersWithDamageKHR", (void*)hook_eglSwapBuffersWithDamageKHR, nullptr);
    void* h4 = shadowhook_hook_sym_name(lib_name.c_str(), "eglGetProcAddress", (void*)hook_eglGetProcAddress, nullptr);
    void* h5 = shadowhook_hook_sym_name(lib_name.c_str(), "dlsym", (void*)hook_dlsym, nullptr);
    void* h6 = shadowhook_hook_sym_name(lib_name.c_str(), "AInputQueue_getEvent", (void*)hook_AInputQueue_getEvent, nullptr);

    LOGD("ShadowHook results for %s: eglSwapBuffers=%p, eglGetProcAddress=%p, dlsym=%p, AInputQueue=%p", lib_name.c_str(), h1, h4, h5, h6);

    if (lib_name == "libil2cpp.so" || lib_name == "libunity.so") {
        IL2CPPTouchHook::InstallHooks();
    }
}

static void library_monitor_thread_func() {
    std::vector<std::string> target_libs = {
        "libunity.so",
        "libil2cpp.so",
        "libcocos2dcpp.so",
        "libhwui.so",
        "libUE4.so",
        "libUnreal.so",
        "libflutter.so",
        "libmonosgen-2.0.so",
        "libGLESv2.so",
        "libGLESv3.so"
    };

    while (true) {
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));

        for (const auto& lib : target_libs) {
            {
                std::lock_guard<std::mutex> lock(g_HookedMutex);
                if (g_HookedLibraries.find(lib) != g_HookedLibraries.end()) {
                    continue; // Already hooked
                }
            }

            if (is_library_loaded(lib)) {
                apply_hooks_to_library(lib);
                {
                    std::lock_guard<std::mutex> lock(g_HookedMutex);
                    g_HookedLibraries.insert(lib);
                }
            }
        }
    }
}

void ImGuiRenderEngine::setGuestState(GuestRenderState state) {
    g_GuestState.store(state);
}

GuestRenderState ImGuiRenderEngine::getGuestState() {
    return g_GuestState.load();
}

ImGuiState ImGuiRenderEngine::getState() {
    return g_ImGuiState.load();
}

void ImGuiRenderEngine::init(const char* packageName) {
    if (!isVirtualGuestProcess()) {
        LOGD("ImGuiRenderEngine: Rejected init. Process '%s' is NOT a virtual guest process (Host process IMGUI is strictly forbidden).", GetCurrentProcessName().c_str());
        g_RenderEnabled.store(false);
        g_ImGuiState.store(ImGuiState::Disabled);
        g_GuestState.store(GuestRenderState::NoGuest);
        return;
    }

    if (packageName == nullptr || strlen(packageName) == 0) {
        LOGD("ImGuiRenderEngine: Skipping init because package name is null or empty");
        g_RenderEnabled.store(false);
        g_ImGuiState.store(ImGuiState::Disabled);
        g_GuestState.store(GuestRenderState::NoGuest);
        return;
    }

    g_TargetAppPackage = packageName;
    
    // Strict native-side whitelist of cloned apps
    bool isWhitelisted = (g_TargetAppPackage == "com.mobile.legends" ||
                         g_TargetAppPackage == "com.tencent.ig" ||
                         g_TargetAppPackage == "com.pubg.imobile" ||
                         g_TargetAppPackage == "com.dts.freefireth" ||
                         g_TargetAppPackage == "com.dts.freefirebg" ||
                         g_TargetAppPackage == "com.miHoYo.GenshinImpact" ||
                         g_TargetAppPackage == "com.tencent.tmgp.sgame" ||
                         g_TargetAppPackage == "com.riotgames.league.wildrift" ||
                         g_TargetAppPackage == "com.activision.callofduty.shooter" ||
                         g_TargetAppPackage == "com.garena.game.codm");

    if (!isWhitelisted) {
        LOGD("ImGuiRenderEngine: Package %s is not in the whitelist. Skipping overlay initialization.", g_TargetAppPackage.c_str());
        g_RenderEnabled.store(false);
        g_ImGuiState.store(ImGuiState::Disabled);
        g_GuestState.store(GuestRenderState::NoGuest);
        return;
    }

    g_GuestState.store(GuestRenderState::GuestRunning);
    g_ImGuiState.store(ImGuiState::WaitingForSurface);
    g_RenderEnabled.store(true);

    if (!g_HooksInstalled.exchange(true)) {
        int res = shadowhook_init(SHADOWHOOK_MODE_SHARED, true);
        LOGD("Initializing ImGuiRenderEngine ShadowHook PLT Hooks for whitelisted package: %s (ShadowHook init res: %d)", g_TargetAppPackage.c_str(), res);

        const char* default_libs[] = {
            "libEGL.so",
            "libGLESv2.so",
            "libGLESv3.so",
            "libunity.so",
            "libcocos2dcpp.so",
            "libUE4.so",
            "libUnreal.so",
            "libflutter.so",
            "libmonosgen-2.0.so",
            "libhwui.so"
        };

        for (const auto& lib : default_libs) {
            shadowhook_hook_sym_name(lib, "eglSwapBuffers", (void*)hook_eglSwapBuffers, nullptr);
            shadowhook_hook_sym_name(lib, "eglSwapBuffersWithDamageEXT", (void*)hook_eglSwapBuffersWithDamageEXT, nullptr);
            shadowhook_hook_sym_name(lib, "eglSwapBuffersWithDamageKHR", (void*)hook_eglSwapBuffersWithDamageKHR, nullptr);
            shadowhook_hook_sym_name(lib, "eglGetProcAddress", (void*)hook_eglGetProcAddress, nullptr);
            shadowhook_hook_sym_name(lib, "dlsym", (void*)hook_dlsym, nullptr);
            shadowhook_hook_sym_name(lib, "AInputQueue_getEvent", (void*)hook_AInputQueue_getEvent, nullptr);
        }

        LOGD("Spawning targeted dynamic library monitor thread");
        std::thread(library_monitor_thread_func).detach();
    }

    LOGD("ImGuiRenderEngine ShadowHook PLT Hooks initialized and enabled successfully for guest package: %s", g_TargetAppPackage.c_str());
}

void ImGuiRenderEngine::setEnabled(bool enabled) {
    if (!isVirtualGuestProcess()) {
        g_RenderEnabled.store(false);
        return;
    }

    if (g_GuestState.load() != GuestRenderState::GuestRunning) {
        g_RenderEnabled.store(false);
        return;
    }

    if (g_TargetAppPackage == "VirtualContainer.Admin" || 
        g_TargetAppPackage == "com.equinox.virtual" || 
        g_TargetAppPackage == "top.niunaijun.blackbox.app" ||
        g_TargetAppPackage == "com.aistudio.equinox" ||
        g_TargetAppPackage.empty()) {
        g_RenderEnabled.store(false);
        return;
    }

    g_RenderEnabled.store(enabled);
}

bool ImGuiRenderEngine::isEnabled() {
    return isVirtualGuestProcess() && g_RenderEnabled.load() && (g_ImGuiState.load() == ImGuiState::Ready || g_ImGuiState.load() == ImGuiState::WaitingForSurface);
}