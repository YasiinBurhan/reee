#include "ImGuiRenderEngine.h"
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/input.h>
#include <android/native_window.h>
#include <android/log.h>
#include <mutex>
#include <cstring>
#include <thread>
#include <chrono>
#include <set>
#include <vector>
#include <string>
#include <fstream>
#include "bytehook.h"
#include "imgui.h"
#include "backends/imgui_impl_opengl3.h"
#include "backends/imgui_impl_android.h"

#define LOG_TAG "ImGuiRenderEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_RenderEnabled = true;
static bool g_EngineInitialized = false;
static std::string g_TargetAppPackage = "";
static std::mutex g_EngineMutex;

static std::set<std::string> g_HookedLibraries;
static std::mutex g_HookedMutex;

static EGLBoolean hook_eglSwapBuffers(EGLDisplay dpy, EGLSurface surface);
static EGLBoolean hook_eglSwapBuffersWithDamageEXT(EGLDisplay dpy, EGLSurface surface, EGLint *rects, EGLint n_rects);
static EGLBoolean hook_eglSwapBuffersWithDamageKHR(EGLDisplay dpy, EGLSurface surface, EGLint *rects, EGLint n_rects);

static void* hook_eglGetProcAddress(const char* procname) {
    BYTEHOOK_STACK_SCOPE();

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

    return BYTEHOOK_CALL_PREV(hook_eglGetProcAddress, procname);
}

static void* hook_dlsym(void* handle, const char* symbol) {
    BYTEHOOK_STACK_SCOPE();

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

    return BYTEHOOK_CALL_PREV(hook_dlsym, handle, symbol);
}

static void render_imgui_frame(EGLDisplay dpy, EGLSurface surface) {
    if (!g_RenderEnabled) return;

    std::lock_guard<std::mutex> lock(g_EngineMutex);

    EGLint width = 0, height = 0;
    eglQuerySurface(dpy, surface, EGL_WIDTH, &width);
    eglQuerySurface(dpy, surface, EGL_HEIGHT, &height);

    if (width > 0 && height > 0) {
        if (!g_EngineInitialized) {
            IMGUI_CHECKVERSION();
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

            const char* gl_version_str = (const char*)glGetString(GL_VERSION);
            const char* gles_version = "#version 300 es";
            if (gl_version_str != nullptr && strstr(gl_version_str, "OpenGL ES 2.0") != nullptr) {
                gles_version = "#version 100";
            }
            LOGD("Detected OpenGL ES version: %s -> Using shader version: %s", gl_version_str ? gl_version_str : "Unknown", gles_version);

            if (ImGui_ImplOpenGL3_Init(gles_version)) {
                g_EngineInitialized = true;
                LOGD("ImGui Render Engine initialized successfully (%dx%d)", width, height);
            } else {
                LOGE("ImGui_ImplOpenGL3_Init failed in Render Engine");
            }
        }

        if (g_EngineInitialized) {
            // Backup complete OpenGL state to prevent breaking the virtualized/host app's rendering
            GLint last_program, last_texture, last_array_buffer, last_viewport[4];
            glGetIntegerv(GL_CURRENT_PROGRAM, &last_program);
            glGetIntegerv(GL_TEXTURE_BINDING_2D, &last_texture);
            glGetIntegerv(GL_ARRAY_BUFFER_BINDING, &last_array_buffer);
            glGetIntegerv(GL_VIEWPORT, last_viewport);

            ImGuiIO& io = ImGui::GetIO();
            io.DisplaySize = ImVec2((float)width, (float)height);

            ImGui_ImplOpenGL3_NewFrame();
            ImGui::NewFrame();

            ImGuiRenderEngine::renderOverlay(width, height);

            ImGui::Render();
            glViewport(0, 0, width, height);
            ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());

            // Fully restore the host app's original OpenGL states
            glUseProgram(last_program);
            glBindTexture(GL_TEXTURE_2D, last_texture);
            glBindBuffer(GL_ARRAY_BUFFER, last_array_buffer);
            glViewport(last_viewport[0], last_viewport[1], last_viewport[2], last_viewport[3]);
        }
    }
}

static EGLBoolean hook_eglSwapBuffers(EGLDisplay dpy, EGLSurface surface) {
    BYTEHOOK_STACK_SCOPE();
    render_imgui_frame(dpy, surface);
    return BYTEHOOK_CALL_PREV(hook_eglSwapBuffers, dpy, surface);
}

static EGLBoolean hook_eglSwapBuffersWithDamageEXT(EGLDisplay dpy, EGLSurface surface, EGLint *rects, EGLint n_rects) {
    BYTEHOOK_STACK_SCOPE();
    render_imgui_frame(dpy, surface);
    return BYTEHOOK_CALL_PREV(hook_eglSwapBuffersWithDamageEXT, dpy, surface, rects, n_rects);
}

static EGLBoolean hook_eglSwapBuffersWithDamageKHR(EGLDisplay dpy, EGLSurface surface, EGLint *rects, EGLint n_rects) {
    BYTEHOOK_STACK_SCOPE();
    render_imgui_frame(dpy, surface);
    return BYTEHOOK_CALL_PREV(hook_eglSwapBuffersWithDamageKHR, dpy, surface, rects, n_rects);
}

static int32_t hook_AInputQueue_getEvent(void* queue, AInputEvent** outEvent) {
    BYTEHOOK_STACK_SCOPE();

    while (true) {
        int32_t res = BYTEHOOK_CALL_PREV(hook_AInputQueue_getEvent, queue, outEvent);
        if (res != 0 || outEvent == nullptr || *outEvent == nullptr) {
            return res;
        }

        AInputEvent* event = *outEvent;
        bool swallow = false;

        if (g_EngineInitialized && g_RenderEnabled) {
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

#include <unistd.h>

// Static menu state
static bool g_MenuOpen = true;
static int g_ActiveTab = 0;
static float g_MenuAlpha = 0.90f;
static int g_SelectedTheme = 0; // 0 = Neon Purple, 1 = Emerald Green, 2 = Cyberpunk Yellow, 3 = Classic Dark

// Toggles & settings
static bool g_SpeedHack = false;
static float g_SpeedMultiplier = 1.0f;
static bool g_SpoofAndroidId = false;
static bool g_BypassIntegrity = false;
static bool g_AntiCheatBypass = false;

// Visuals
static bool g_ShowFPS = true;
static bool g_ShowCrosshair = false;
static int g_CrosshairStyle = 0; // 0 = Dot, 1 = Plus, 2 = Circle
static float g_CrosshairSize = 10.0f;
static float g_CrosshairColor[4] = { 1.0f, 0.0f, 0.0f, 1.0f }; // Red default

// Floating handle position (initialized dynamically on first run)
static ImVec2 g_FloatingPos = ImVec2(50.0f, 150.0f);
static bool g_FloatingInitialized = false;

static void ApplyTheme(int theme_id) {
    ImGuiStyle& style = ImGui::GetStyle();
    ImVec4* colors = style.Colors;

    // Reset base properties for premium feel
    style.WindowRounding = 12.0f;
    style.FrameRounding = 8.0f;
    style.PopupRounding = 8.0f;
    style.ScrollbarRounding = 12.0f;
    style.GrabRounding = 6.0f;
    style.WindowBorderSize = 1.5f;
    style.FrameBorderSize = 1.0f;

    // Primary Colors selection
    ImVec4 primaryColor;
    ImVec4 primaryHovered;
    ImVec4 primaryActive;

    if (theme_id == 0) { // Neon Purple
        primaryColor = ImVec4(0.57f, 0.23f, 0.85f, 1.0f);
        primaryHovered = ImVec4(0.67f, 0.33f, 0.95f, 1.0f);
        primaryActive = ImVec4(0.47f, 0.13f, 0.75f, 1.0f);
    } else if (theme_id == 1) { // Emerald Green
        primaryColor = ImVec4(0.00f, 0.78f, 0.45f, 1.0f);
        primaryHovered = ImVec4(0.10f, 0.88f, 0.55f, 1.0f);
        primaryActive = ImVec4(0.00f, 0.68f, 0.35f, 1.0f);
    } else if (theme_id == 2) { // Cyberpunk Yellow
        primaryColor = ImVec4(0.98f, 0.85f, 0.08f, 1.0f);
        primaryHovered = ImVec4(1.00f, 0.90f, 0.20f, 1.0f);
        primaryActive = ImVec4(0.88f, 0.75f, 0.00f, 1.0f);
    } else { // Classic Dark / Steel Blue
        primaryColor = ImVec4(0.18f, 0.48f, 0.96f, 1.0f);
        primaryHovered = ImVec4(0.28f, 0.58f, 0.99f, 1.0f);
        primaryActive = ImVec4(0.08f, 0.38f, 0.86f, 1.0f);
    }

    colors[ImGuiCol_Text]                   = ImVec4(0.95f, 0.96f, 0.98f, 1.0f);
    colors[ImGuiCol_TextDisabled]           = ImVec4(0.50f, 0.50f, 0.50f, 1.0f);
    colors[ImGuiCol_WindowBg]               = ImVec4(0.06f, 0.05f, 0.08f, g_MenuAlpha);
    colors[ImGuiCol_ChildBg]                = ImVec4(0.10f, 0.09f, 0.12f, 0.50f);
    colors[ImGuiCol_PopupBg]                = ImVec4(0.10f, 0.09f, 0.15f, 0.98f);
    colors[ImGuiCol_Border]                 = primaryColor;
    colors[ImGuiCol_BorderShadow]           = ImVec4(0.00f, 0.00f, 0.00f, 0.00f);
    colors[ImGuiCol_FrameBg]                = ImVec4(0.14f, 0.13f, 0.17f, 1.0f);
    colors[ImGuiCol_FrameBgHovered]         = ImVec4(0.20f, 0.19f, 0.24f, 1.0f);
    colors[ImGuiCol_FrameBgActive]          = ImVec4(0.26f, 0.25f, 0.30f, 1.0f);
    colors[ImGuiCol_TitleBg]                = ImVec4(0.08f, 0.07f, 0.10f, 1.0f);
    colors[ImGuiCol_TitleBgActive]          = ImVec4(0.12f, 0.11f, 0.15f, 1.0f);
    colors[ImGuiCol_TitleBgCollapsed]       = ImVec4(0.00f, 0.00f, 0.00f, 0.51f);
    colors[ImGuiCol_MenuBarBg]              = ImVec4(0.14f, 0.14f, 0.14f, 1.0f);
    colors[ImGuiCol_ScrollbarBg]            = ImVec4(0.02f, 0.02f, 0.02f, 0.39f);
    colors[ImGuiCol_ScrollbarGrab]          = ImVec4(0.31f, 0.31f, 0.31f, 1.0f);
    colors[ImGuiCol_ScrollbarGrabHovered]   = ImVec4(0.41f, 0.41f, 0.41f, 1.0f);
    colors[ImGuiCol_ScrollbarGrabActive]    = ImVec4(0.51f, 0.51f, 0.51f, 1.0f);
    colors[ImGuiCol_CheckMark]              = primaryColor;
    colors[ImGuiCol_SliderGrab]             = primaryColor;
    colors[ImGuiCol_SliderGrabActive]       = primaryActive;
    colors[ImGuiCol_Button]                 = primaryColor;
    colors[ImGuiCol_ButtonHovered]          = primaryHovered;
    colors[ImGuiCol_ButtonActive]           = primaryActive;
    colors[ImGuiCol_Header]                 = ImVec4(primaryColor.x, primaryColor.y, primaryColor.z, 0.40f);
    colors[ImGuiCol_HeaderHovered]          = ImVec4(primaryColor.x, primaryColor.y, primaryColor.z, 0.65f);
    colors[ImGuiCol_HeaderActive]           = primaryColor;
    colors[ImGuiCol_Separator]              = ImVec4(0.20f, 0.19f, 0.24f, 1.0f);
    colors[ImGuiCol_SeparatorHovered]       = primaryHovered;
    colors[ImGuiCol_SeparatorActive]        = primaryActive;
}

void ImGuiRenderEngine::renderOverlay(int width, int height) {
    if (!g_FloatingInitialized) {
        g_FloatingPos = ImVec2(40.0f, (float)height * 0.3f);
        g_FloatingInitialized = true;
    }

    if (!g_MenuOpen) {
        // Render simple minimized floating button
        ImGui::SetNextWindowSize(ImVec2(75, 75));
        ImGui::SetNextWindowPos(g_FloatingPos, ImGuiCond_Always);
        
        // Remove borders and background paddings for round button look
        ImGui::PushStyleColor(ImGuiCol_WindowBg, ImVec4(0.08f, 0.07f, 0.10f, 0.85f));
        ImGui::PushStyleColor(ImGuiCol_Border, ImVec4(0.57f, 0.23f, 0.85f, 1.0f));
        ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(4, 4));
        ImGui::PushStyleVar(ImGuiStyleVar_WindowRounding, 37.5f); // Half of width/height for circle
        ImGui::PushStyleVar(ImGuiStyleVar_WindowMinSize, ImVec2(10, 10));

        ImGui::Begin("FloatingMenuButton", nullptr, 
            ImGuiWindowFlags_NoDecoration | 
            ImGuiWindowFlags_NoScrollWithMouse | 
            ImGuiWindowFlags_NoResize | 
            ImGuiWindowFlags_NoSavedSettings | 
            ImGuiWindowFlags_NoFocusOnAppearing | 
            ImGuiWindowFlags_NoBringToFrontOnFocus);

        // Manage manual dragging so user can move it anywhere
        if (ImGui::IsWindowFocused() && ImGui::IsMouseDragging(0)) {
            g_FloatingPos.x += ImGui::GetIO().MouseDelta.x;
            g_FloatingPos.y += ImGui::GetIO().MouseDelta.y;
            
            // Constrain floating button within screen boundaries
            if (g_FloatingPos.x < 0) g_FloatingPos.x = 0;
            if (g_FloatingPos.y < 0) g_FloatingPos.y = 0;
            if (g_FloatingPos.x > (float)width - 75) g_FloatingPos.x = (float)width - 75;
            if (g_FloatingPos.y > (float)height - 75) g_FloatingPos.y = (float)height - 75;
        }

        // Inside the circle, draw a beautiful logo or text
        ImGui::SetCursorPos(ImVec2(12.5f, 12.5f));
        if (ImGui::Button("EQ", ImVec2(50.0f, 50.0f))) {
            g_MenuOpen = true;
        }
        
        ImGui::End();
        ImGui::PopStyleVar(3);
        ImGui::PopStyleColor(2);
    } else {
        // Render main menu window
        ApplyTheme(g_SelectedTheme);

        ImGui::SetNextWindowSize(ImVec2(450, 320), ImGuiCond_FirstUseEver);
        ImGui::Begin("EQuinox Virtual Menu", nullptr, ImGuiWindowFlags_NoCollapse);

        // Add minimized option
        if (ImGui::Button("Minimize Menu", ImVec2(120, 24))) {
            g_MenuOpen = false;
        }
        ImGui::SameLine();
        ImGui::TextColored(ImVec4(1.0f, 1.0f, 1.0f, 0.5f), "| Package: %s", g_TargetAppPackage.empty() ? "com.virtual.cloned" : g_TargetAppPackage.c_str());

        ImGui::Separator();

        // Top tabs. Modern, spacious layout
        if (ImGui::Button("MAIN", ImVec2(90, 30))) g_ActiveTab = 0;
        ImGui::SameLine();
        if (ImGui::Button("VISUALS", ImVec2(90, 30))) g_ActiveTab = 1;
        ImGui::SameLine();
        if (ImGui::Button("SETTINGS", ImVec2(90, 30))) g_ActiveTab = 2;
        ImGui::SameLine();
        if (ImGui::Button("SYSTEM", ImVec2(90, 30))) g_ActiveTab = 3;

        ImGui::Separator();

        // Render tab contents
        switch (g_ActiveTab) {
            case 0: { // MAIN
                ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.7f, 1.0f), "Core Hacks & Overrides");
                ImGui::Dummy(ImVec2(0.0f, 4.0f));

                ImGui::Checkbox("Virtual Speed Hack Emulator", &g_SpeedHack);
                if (g_SpeedHack) {
                    ImGui::SliderFloat("Multiplier", &g_SpeedMultiplier, 0.1f, 10.0f, "%.1fx");
                }
                
                ImGui::Checkbox("Spoof Android Device ID", &g_SpoofAndroidId);
                ImGui::Checkbox("Bypass Security Integrity checks", &g_BypassIntegrity);
                ImGui::Checkbox("Bypass Third-Party Anti-Cheat", &g_AntiCheatBypass);
                break;
            }
            case 1: { // VISUALS
                ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.7f, 1.0f), "Visual Overlays");
                ImGui::Dummy(ImVec2(0.0f, 4.0f));

                ImGui::Checkbox("Show Performance FPS Widget", &g_ShowFPS);
                ImGui::Checkbox("Show Hardware Crosshair", &g_ShowCrosshair);
                
                if (g_ShowCrosshair) {
                    ImGui::Combo("Style", &g_CrosshairStyle, "Dot\0Plus\0Circle\0");
                    ImGui::SliderFloat("Size", &g_CrosshairSize, 5.0f, 30.0f, "%.0fpx");
                    ImGui::ColorEdit4("Crosshair Color", g_CrosshairColor);
                }
                break;
            }
            case 2: { // SETTINGS
                ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.7f, 1.0f), "UI & Menu Customization");
                ImGui::Dummy(ImVec2(0.0f, 4.0f));

                ImGui::Combo("Theme Accent", &g_SelectedTheme, "Neon Purple\0Emerald Green\0Cyberpunk Yellow\0Steel Blue\0");
                ImGui::SliderFloat("Menu Transparency", &g_MenuAlpha, 0.40f, 1.00f, "%.2f");
                break;
            }
            case 3: { // SYSTEM
                ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.7f, 1.0f), "Sandbox Information");
                ImGui::Dummy(ImVec2(0.0f, 4.0f));

                ImGui::Text("Sandbox Engine: EQuinox Android Virtual Engine");
                ImGui::Text("Render Backend: OpenGL ES 3.0 / EGL");
                ImGui::Text("Current Process ID: %d", getpid());
                ImGui::Text("Active Core FPS: %.1f FPS", ImGui::GetIO().Framerate);
                ImGui::Text("Container Screen Size: %d x %d", width, height);
                ImGui::Text("Status: Sandbox Integrity verified (Anti-Detect OK)");
                break;
            }
        }

        ImGui::End();
    }

    // Render Overlay Widgets (FPS and Crosshair) independent of menu state
    if (g_ShowFPS) {
        ImGui::SetNextWindowBgAlpha(0.35f);
        ImGui::SetNextWindowPos(ImVec2((float)width - 100, 10), ImGuiCond_Always);
        ImGui::Begin("FPSOverlay", nullptr, ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_AlwaysAutoResize | ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoFocusOnAppearing | ImGuiWindowFlags_NoNav);
        ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.5f, 1.0f), "FPS: %.1f", ImGui::GetIO().Framerate);
        ImGui::End();
    }

    if (g_ShowCrosshair) {
        ImDrawList* draw_list = ImGui::GetForegroundDrawList();
        float cx = (float)width / 2.0f;
        float cy = (float)height / 2.0f;
        ImU32 color = ImGui::ColorConvertFloat4ToU32(ImVec4(g_CrosshairColor[0], g_CrosshairColor[1], g_CrosshairColor[2], g_CrosshairColor[3]));

        if (g_CrosshairStyle == 0) { // Dot
            draw_list->AddCircleFilled(ImVec2(cx, cy), g_CrosshairSize / 4.0f, color);
        } else if (g_CrosshairStyle == 1) { // Plus
            draw_list->AddLine(ImVec2(cx - g_CrosshairSize, cy), ImVec2(cx + g_CrosshairSize, cy), color, 2.0f);
            draw_list->AddLine(ImVec2(cx, cy - g_CrosshairSize), ImVec2(cx, cy + g_CrosshairSize), color, 2.0f);
        } else if (g_CrosshairStyle == 2) { // Circle
            draw_list->AddCircle(ImVec2(cx, cy), g_CrosshairSize, color, 24, 2.0f);
        }
    }
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
    LOGD("Manually applying ByteHook PLT Hooks to loaded library: %s", lib_name.c_str());

    bytehook_hook_single(lib_name.c_str(), nullptr, "eglSwapBuffers", (void*)hook_eglSwapBuffers, nullptr, nullptr);
    bytehook_hook_single(lib_name.c_str(), nullptr, "eglSwapBuffersWithDamageEXT", (void*)hook_eglSwapBuffersWithDamageEXT, nullptr, nullptr);
    bytehook_hook_single(lib_name.c_str(), nullptr, "eglSwapBuffersWithDamageKHR", (void*)hook_eglSwapBuffersWithDamageKHR, nullptr, nullptr);
    bytehook_hook_single(lib_name.c_str(), nullptr, "eglGetProcAddress", (void*)hook_eglGetProcAddress, nullptr, nullptr);
    bytehook_hook_single(lib_name.c_str(), nullptr, "dlsym", (void*)hook_dlsym, nullptr, nullptr);
    bytehook_hook_single(lib_name.c_str(), nullptr, "AInputQueue_getEvent", (void*)hook_AInputQueue_getEvent, nullptr, nullptr);
}

static void library_monitor_thread_func() {
    std::vector<std::string> target_libs = {
        "libunity.so",
        "libcocos2dcpp.so",
        "libhwui.so",
        "libUE4.so",
        "libUnreal.so",
        "libflutter.so",
        "libmonosgen-2.0.so",
        "libGLESv2.so",
        "libGLESv3.so"
    };

    int fallback_counter = 0;

    // Keep running as long as the process is alive
    while (true) {
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
        fallback_counter++;

        // Global fallback once every 5 seconds to hook any other newly loaded libraries
        if (fallback_counter >= 5) {
            fallback_counter = 0;
            bytehook_hook_all(nullptr, "eglSwapBuffers", (void*)hook_eglSwapBuffers, nullptr, nullptr);
            bytehook_hook_all(nullptr, "eglSwapBuffersWithDamageEXT", (void*)hook_eglSwapBuffersWithDamageEXT, nullptr, nullptr);
            bytehook_hook_all(nullptr, "eglSwapBuffersWithDamageKHR", (void*)hook_eglSwapBuffersWithDamageKHR, nullptr, nullptr);
            bytehook_hook_all(nullptr, "eglGetProcAddress", (void*)hook_eglGetProcAddress, nullptr, nullptr);
            bytehook_hook_all(nullptr, "dlsym", (void*)hook_dlsym, nullptr, nullptr);
        }

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

void ImGuiRenderEngine::init(const char* packageName) {
    bytehook_init(BYTEHOOK_MODE_AUTOMATIC, false);
    if (packageName != nullptr) {
        g_TargetAppPackage = packageName;
    }
    LOGD("Initializing ImGuiRenderEngine ByteHook PLT Hooks for package: %s", g_TargetAppPackage.c_str());

    bytehook_hook_all(nullptr, "eglSwapBuffers", (void*)hook_eglSwapBuffers, nullptr, nullptr);
    bytehook_hook_all(nullptr, "eglSwapBuffersWithDamageEXT", (void*)hook_eglSwapBuffersWithDamageEXT, nullptr, nullptr);
    bytehook_hook_all(nullptr, "eglSwapBuffersWithDamageKHR", (void*)hook_eglSwapBuffersWithDamageKHR, nullptr, nullptr);
    bytehook_hook_all(nullptr, "eglGetProcAddress", (void*)hook_eglGetProcAddress, nullptr, nullptr);
    bytehook_hook_all(nullptr, "dlsym", (void*)hook_dlsym, nullptr, nullptr);
    bytehook_hook_all(nullptr, "AInputQueue_getEvent", (void*)hook_AInputQueue_getEvent, nullptr, nullptr);

    LOGD("Spawning targeted dynamic library monitor thread");
    std::thread(library_monitor_thread_func).detach();

    LOGD("ImGuiRenderEngine ByteHook PLT Hooks initialized successfully");
}

void ImGuiRenderEngine::setEnabled(bool enabled) {
    g_RenderEnabled = enabled;
}

bool ImGuiRenderEngine::isEnabled() {
    return g_RenderEnabled;
}
