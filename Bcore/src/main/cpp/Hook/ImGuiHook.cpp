#include "ImGuiHook.h"
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/input.h>
#include <android/native_window.h>
#include <android/log.h>
#include <string>
#include <mutex>
#include "bytehook.h"
#include "imgui.h"
#include "backends/imgui_impl_opengl3.h"
#include "backends/imgui_impl_android.h"

#define LOG_TAG "ImGuiSurfaceHook"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_HookEnabled = true;
static bool g_ImGuiInitialized = false;
static std::string g_TargetPackage = "";
static std::mutex g_InitMutex;

// Hook callback for eglSwapBuffers
static EGLBoolean my_eglSwapBuffers(EGLDisplay dpy, EGLSurface surface) {
    BYTEHOOK_STACK_SCOPE();

    if (g_HookEnabled) {
        std::lock_guard<std::mutex> lock(g_InitMutex);

        EGLint width = 0, height = 0;
        eglQuerySurface(dpy, surface, EGL_WIDTH, &width);
        eglQuerySurface(dpy, surface, EGL_HEIGHT, &height);

        if (width > 0 && height > 0) {
            if (!g_ImGuiInitialized) {
                IMGUI_CHECKVERSION();
                ImGui::CreateContext();
                ImGuiIO& io = ImGui::GetIO();
                io.IniFilename = nullptr; // Disable config file saving

                // Styling for mobile virtual surface overlay
                ImGui::StyleColorsDark();
                ImGuiStyle& style = ImGui::GetStyle();
                style.WindowRounding = 12.0f;
                style.FrameRounding = 8.0f;
                style.PopupRounding = 8.0f;
                style.ScrollbarRounding = 8.0f;
                style.GrabRounding = 6.0f;
                style.WindowBorderSize = 1.0f;
                style.ScaleAllSizes(1.5f);

                if (ImGui_ImplOpenGL3_Init("#version 300 es")) {
                    g_ImGuiInitialized = true;
                    LOGD("ImGui surface canvas initialized successfully (%dx%d)", width, height);
                } else {
                    LOGE("ImGui_ImplOpenGL3_Init failed");
                }
            }

            if (g_ImGuiInitialized) {
                // Save OpenGL State
                GLint last_program, last_texture, last_array_buffer, last_viewport[4];
                glGetIntegerv(GL_CURRENT_PROGRAM, &last_program);
                glGetIntegerv(GL_TEXTURE_BINDING_2D, &last_texture);
                glGetIntegerv(GL_ARRAY_BUFFER_BINDING, &last_array_buffer);
                glGetIntegerv(GL_VIEWPORT, last_viewport);

                ImGuiIO& io = ImGui::GetIO();
                io.DisplaySize = ImVec2((float)width, (float)height);

                ImGui_ImplOpenGL3_NewFrame();
                ImGui::NewFrame();

                // Virtual Canvas ImGui Overlay Window
                ImGui::SetNextWindowSize(ImVec2(340, 260), ImGuiCond_FirstUseEver);
                ImGui::Begin("Virtual Surface Canvas", nullptr, ImGuiWindowFlags_NoCollapse);

                ImGui::TextColored(ImVec4(0.2f, 0.8f, 1.0f, 1.0f), "Mode: Virtual EGL Surface Canvas");
                ImGui::Text("Package: %s", g_TargetPackage.empty() ? "com.virtual.app" : g_TargetPackage.c_str());
                ImGui::Text("FPS: %.1f", io.Framerate);
                ImGui::Text("Resolution: %dx%d", width, height);
                ImGui::Separator();

                static bool feature_watermark = true;
                static bool touch_forwarding = true;
                static float menu_alpha = 0.9f;

                ImGui::Checkbox("Draw Surface Watermark", &feature_watermark);
                ImGui::Checkbox("Intercept & Forward Touch Events", &touch_forwarding);
                ImGui::SliderFloat("Surface Alpha", &menu_alpha, 0.3f, 1.0f);

                if (feature_watermark) {
                    ImDrawList* draw_list = ImGui::GetBackgroundDrawList();
                    draw_list->AddText(ImVec2(24.0f, height - 48.0f),
                        IM_COL32(0, 255, 128, 220), "Virtual Canvas Overlay Running (No SYSTEM_ALERT_WINDOW required)");
                }

                ImGui::End();

                ImGui::Render();
                glViewport(0, 0, width, height);
                ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());

                // Restore OpenGL State
                glUseProgram(last_program);
                glBindTexture(GL_TEXTURE_2D, last_texture);
                glBindBuffer(GL_ARRAY_BUFFER, last_array_buffer);
                glViewport(last_viewport[0], last_viewport[1], last_viewport[2], last_viewport[3]);
            }
        }
    }

    return BYTEHOOK_CALL_PREV(my_eglSwapBuffers, dpy, surface);
}

// Hook callback for AInputQueue_getEvent
static int32_t my_AInputQueue_getEvent(void* queue, AInputEvent** outEvent) {
    BYTEHOOK_STACK_SCOPE();

    int32_t res = BYTEHOOK_CALL_PREV(my_AInputQueue_getEvent, queue, outEvent);
    if (res == 0 && outEvent != nullptr && *outEvent != nullptr) {
        AInputEvent* event = *outEvent;
        if (g_ImGuiInitialized && g_HookEnabled) {
            ImGui_ImplAndroid_HandleInputEvent(event);
        }
    }
    return res;
}

void ImGuiHook::init(const char* packageName) {
    bytehook_init(BYTEHOOK_MODE_AUTOMATIC, false);
    if (packageName != nullptr) {
        g_TargetPackage = packageName;
    }
    LOGD("Initializing ImGui Surface PLT Hook with ByteHook for package: %s", g_TargetPackage.c_str());

    // ByteHook eglSwapBuffers across EGL libraries
    bytehook_hook_all(nullptr, "eglSwapBuffers", (void*)my_eglSwapBuffers, nullptr, nullptr);

    // ByteHook AInputQueue_getEvent for touch event intercept
    bytehook_hook_all(nullptr, "AInputQueue_getEvent", (void*)my_AInputQueue_getEvent, nullptr, nullptr);

    LOGD("ImGui Surface PLT Hooks successfully registered!");
}

void ImGuiHook::setEnabled(bool enabled) {
    g_HookEnabled = enabled;
}

bool ImGuiHook::isEnabled() {
    return g_HookEnabled;
}
