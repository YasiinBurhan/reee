#include "IL2CPPTouchHook.h"
#include "imgui.h"
#include <shadowhook.h>
#include <android/log.h>

#define LOG_TAG "IL2CPPTouchHook"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_HooksInstalled = false;

// Original function pointers
static bool (*orig_GetMouseButton)(int button) = nullptr;
static bool (*orig_GetMouseButtonDown)(int button) = nullptr;
static bool (*orig_GetMouseButtonUp)(int button) = nullptr;
static int (*orig_get_touchCount)() = nullptr;
static bool g_IL2CPPInitialized = false;
static Il2CppDomain* (*orig_il2cpp_init)(const char* domain_name) = nullptr;

// Forward declaration of InstallHooks
bool InstallHooks_Internal();

static Il2CppDomain* hook_il2cpp_init(const char* domain_name) {
    LOGI("hook_il2cpp_init: il2cpp_init called with domain_name: %s", domain_name);
    Il2CppDomain* domain = orig_il2cpp_init(domain_name);
    
    g_IL2CPPInitialized = true;
    LOGI("hook_il2cpp_init: il2cpp_init completed. Installing deferred touch hooks.");
    IL2CPPTouchHook::InstallHooks();
    
    return domain;
}

// Hook functions
static bool hook_GetMouseButton(int button) {
    if (ImGui::GetCurrentContext() != nullptr) {
        if (ImGui::GetIO().WantCaptureMouse || ImGui::GetIO().WantCaptureKeyboard) {
            return false; // Consume touch for ImGui, block Unity game touch
        }
    }
    return orig_GetMouseButton ? orig_GetMouseButton(button) : false;
}

static bool hook_GetMouseButtonDown(int button) {
    if (ImGui::GetCurrentContext() != nullptr) {
        if (ImGui::GetIO().WantCaptureMouse || ImGui::GetIO().WantCaptureKeyboard) {
            return false;
        }
    }
    return orig_GetMouseButtonDown ? orig_GetMouseButtonDown(button) : false;
}

static bool hook_GetMouseButtonUp(int button) {
    if (ImGui::GetCurrentContext() != nullptr) {
        if (ImGui::GetIO().WantCaptureMouse || ImGui::GetIO().WantCaptureKeyboard) {
            return false;
        }
    }
    return orig_GetMouseButtonUp ? orig_GetMouseButtonUp(button) : false;
}

static int hook_get_touchCount() {
    if (ImGui::GetCurrentContext() != nullptr) {
        if (ImGui::GetIO().WantCaptureMouse || ImGui::GetIO().WantCaptureKeyboard) {
            return 0; // Return 0 touches to Unity when ImGui menu is being touched
        }
    }
    return orig_get_touchCount ? orig_get_touchCount() : 0;
}

bool IL2CPPTouchHook::InstallHooks() {
    if (g_HooksInstalled) return true;

    if (!g_IL2CPPInitialized) {
        if (orig_il2cpp_init == nullptr) {
            void* stub = shadowhook_hook_sym_name("libil2cpp.so", "il2cpp_init", (void*)hook_il2cpp_init, (void**)&orig_il2cpp_init);
            if (stub) {
                LOGI("Deferring IL2CPP touch hook installation: Successfully hooked il2cpp_init");
                return true;
            } else {
                LOGE("Failed to hook il2cpp_init for deferring touch hooks.");
            }
        } else {
            LOGI("Touch hook installation is already deferred on il2cpp_init.");
            return true;
        }
        return false;
    }

    if (!IL2CPPAPI::Init("libil2cpp.so")) {
        LOGE("IL2CPPAPI Init failed. Cannot install IL2CPP touch hooks.");
        return false;
    }

    LOGI("il2cpp is fully initialized. Installing touch hooks now...");

    // Resolve Unity Input functions via il2cpp_resolve_icall or GetMethodPointer
    void* addr_GetMouseButton = nullptr;
    void* addr_GetMouseButtonDown = nullptr;
    void* addr_GetMouseButtonUp = nullptr;
    void* addr_get_touchCount = nullptr;

    if (il2cpp_resolve_icall) {
        addr_GetMouseButton = il2cpp_resolve_icall("UnityEngine.Input::GetMouseButton(System.Int32)");
        if (!addr_GetMouseButton) {
            addr_GetMouseButton = il2cpp_resolve_icall("UnityEngine.Input::GetMouseButton");
        }

        addr_GetMouseButtonDown = il2cpp_resolve_icall("UnityEngine.Input::GetMouseButtonDown(System.Int32)");
        if (!addr_GetMouseButtonDown) {
            addr_GetMouseButtonDown = il2cpp_resolve_icall("UnityEngine.Input::GetMouseButtonDown");
        }

        addr_GetMouseButtonUp = il2cpp_resolve_icall("UnityEngine.Input::GetMouseButtonUp(System.Int32)");
        if (!addr_GetMouseButtonUp) {
            addr_GetMouseButtonUp = il2cpp_resolve_icall("UnityEngine.Input::GetMouseButtonUp");
        }

        addr_get_touchCount = il2cpp_resolve_icall("UnityEngine.Input::get_touchCount");
    }

    // Fallback to GetMethodPointer if icall is null
    if (!addr_GetMouseButton) {
        addr_GetMouseButton = IL2CPPAPI::GetMethodPointer("UnityEngine.InputModule", "UnityEngine", "Input", "GetMouseButton", 1);
        if (!addr_GetMouseButton) {
            addr_GetMouseButton = IL2CPPAPI::GetMethodPointer("UnityEngine", "UnityEngine", "Input", "GetMouseButton", 1);
        }
    }

    if (!addr_GetMouseButtonDown) {
        addr_GetMouseButtonDown = IL2CPPAPI::GetMethodPointer("UnityEngine.InputModule", "UnityEngine", "Input", "GetMouseButtonDown", 1);
        if (!addr_GetMouseButtonDown) {
            addr_GetMouseButtonDown = IL2CPPAPI::GetMethodPointer("UnityEngine", "UnityEngine", "Input", "GetMouseButtonDown", 1);
        }
    }

    if (!addr_get_touchCount) {
        addr_get_touchCount = IL2CPPAPI::GetMethodPointer("UnityEngine.InputModule", "UnityEngine", "Input", "get_touchCount", 0);
        if (!addr_get_touchCount) {
            addr_get_touchCount = IL2CPPAPI::GetMethodPointer("UnityEngine", "UnityEngine", "Input", "get_touchCount", 0);
        }
    }

    int hookCount = 0;

    if (addr_GetMouseButton) {
        void* stub = shadowhook_hook_func_addr(addr_GetMouseButton, (void*)hook_GetMouseButton, (void**)&orig_GetMouseButton);
        if (stub) {
            LOGI("Successfully hooked UnityEngine.Input::GetMouseButton at %p", addr_GetMouseButton);
            hookCount++;
        }
    }

    if (addr_GetMouseButtonDown) {
        void* stub = shadowhook_hook_func_addr(addr_GetMouseButtonDown, (void*)hook_GetMouseButtonDown, (void**)&orig_GetMouseButtonDown);
        if (stub) {
            LOGI("Successfully hooked UnityEngine.Input::GetMouseButtonDown at %p", addr_GetMouseButtonDown);
            hookCount++;
        }
    }

    if (addr_GetMouseButtonUp) {
        void* stub = shadowhook_hook_func_addr(addr_GetMouseButtonUp, (void*)hook_GetMouseButtonUp, (void**)&orig_GetMouseButtonUp);
        if (stub) {
            LOGI("Successfully hooked UnityEngine.Input::GetMouseButtonUp at %p", addr_GetMouseButtonUp);
            hookCount++;
        }
    }

    if (addr_get_touchCount) {
        void* stub = shadowhook_hook_func_addr(addr_get_touchCount, (void*)hook_get_touchCount, (void**)&orig_get_touchCount);
        if (stub) {
            LOGI("Successfully hooked UnityEngine.Input::get_touchCount at %p", addr_get_touchCount);
            hookCount++;
        }
    }

    g_HooksInstalled = (hookCount > 0);
    LOGI("IL2CPPTouchHook installed: %s (%d hooks active)", g_HooksInstalled ? "YES" : "NO", hookCount);
    return g_HooksInstalled;
}

bool IL2CPPTouchHook::IsInstalled() {
    return g_HooksInstalled;
}
