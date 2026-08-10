#include "ImGuiRenderEngine.h"
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/input.h>
#include <android/native_window.h>
#include <android/log.h>
#include <mutex>
#include <cstring>
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

            if (ImGui_ImplOpenGL3_Init("#version 300 es")) {
                g_EngineInitialized = true;
                LOGD("ImGui Render Engine initialized successfully (%dx%d)", width, height);
            } else {
                LOGE("ImGui_ImplOpenGL3_Init failed in Render Engine");
            }
        }

        if (g_EngineInitialized) {
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

    int32_t res = BYTEHOOK_CALL_PREV(hook_AInputQueue_getEvent, queue, outEvent);
    if (res == 0 && outEvent != nullptr && *outEvent != nullptr) {
        AInputEvent* event = *outEvent;
        if (g_EngineInitialized && g_RenderEnabled) {
            ImGui_ImplAndroid_HandleInputEvent(event);
        }
    }
    return res;
}

void ImGuiRenderEngine::renderOverlay(int width, int height) {
    ImGui::SetNextWindowSize(ImVec2(320, 240), ImGuiCond_FirstUseEver);
    ImGui::Begin("ImGui Render Canvas", nullptr, ImGuiWindowFlags_NoCollapse);

    ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.6f, 1.0f), "Virtual Space App Running");
    ImGui::Text("Package: %s", g_TargetAppPackage.empty() ? "com.virtual.cloned" : g_TargetAppPackage.c_str());
    ImGui::Text("FPS: %.1f", ImGui::GetIO().Framerate);
    ImGui::Text("Canvas Resolution: %dx%d", width, height);
    ImGui::Separator();

    static bool show_demo = false;
    static float bg_alpha = 0.85f;

    ImGui::Checkbox("Show ImGui Demo Window", &show_demo);
    ImGui::SliderFloat("Canvas Alpha", &bg_alpha, 0.2f, 1.0f);

    if (show_demo) {
        ImGui::ShowDemoWindow(&show_demo);
    }

    ImGui::End();
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

    LOGD("ImGuiRenderEngine ByteHook PLT Hooks initialized successfully");
}

void ImGuiRenderEngine::setEnabled(bool enabled) {
    g_RenderEnabled = enabled;
}

bool ImGuiRenderEngine::isEnabled() {
    return g_RenderEnabled;
}
