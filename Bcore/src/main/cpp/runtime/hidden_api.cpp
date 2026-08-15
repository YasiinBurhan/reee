#include <jni.h>
#include <sys/system_properties.h>
#include <cstdlib>
#include <cstring>
#include <mutex>
#include "shadowhook.h"
#include "runtime/hidden_api.h"
#include "utils/Log.h"
#include "utils/JniUtils.h"

namespace blackbox {

bool HiddenApi::disableHiddenApi(JNIEnv *env) {
    char version_str[PROP_VALUE_MAX];
    if (!__system_property_get("ro.build.version.sdk", version_str)) {
        ALOGE("Failed to obtain SDK int");
        return false;
    }
    long android_version = std::strtol(version_str, nullptr, 10);

    if (android_version < 29) {
        ALOGD("HiddenAPI: Android version < 29, no need to disable");
        return true;
    }

    void *addr = nullptr;
    const char* symbol_names[] = {
        "_ZN3artL32VMRuntime_setHiddenApiExemptionsEP7_JNIEnvP7_jclassP13_jobjectArray",
        "_ZN3art9VMRuntime22setHiddenApiExemptionsEP7_JNIEnvP7_jclassP13_jobjectArray",
        "art::VMRuntime::setHiddenApiExemptions(_JNIEnv*, _jclass*, _jobjectArray*)",
        nullptr
    };

    void* handle = shadowhook_dlopen("libart.so");
    if (handle) {
        for (int i = 0; symbol_names[i] != nullptr; i++) {
            addr = shadowhook_dlsym(handle, symbol_names[i]);
            if (addr) {
                ALOGD("HiddenAPI: Found symbol %s via shadowhook at %p", symbol_names[i], addr);
                break;
            }
        }
        shadowhook_dlclose(handle);
    }

    if (!addr) {
        ALOGE("HiddenAPI: Didn't find setHiddenApiExemptions in any form");
        return false;
    }

    ScopedLocalRef<jclass> stringClass(env, env->FindClass("java/lang/String"));
    if (stringClass.empty()) {
        ALOGE("HiddenAPI: Failed to find String class");
        return false;
    }

    ScopedLocalRef<jstring> wildcard(env, env->NewStringUTF("L"));
    if (wildcard.empty()) {
        ALOGE("HiddenAPI: Failed to create wildcard string");
        return false;
    }

    ScopedLocalRef<jobjectArray> args(env, env->NewObjectArray(1, stringClass.get(), wildcard.get()));
    if (args.empty()) {
        ALOGE("HiddenAPI: Failed to create args array");
        return false;
    }

    auto func = reinterpret_cast<void (*)(JNIEnv *, jclass, jobjectArray)>(addr);
    func(env, stringClass.get(), args.get());
    ALOGD("HiddenAPI: Successfully disabled hidden API restrictions");
    return true;
}

static void* s_libandroid_runtime_handle = nullptr;
static std::mutex s_resource_loading_mutex;

enum class ResourceLoadingState {
    Uninitialized,
    Resolving,
    Available,
    Unsupported,
    Failed
};

static ResourceLoadingState s_resource_loading_state = ResourceLoadingState::Uninitialized;

bool HiddenApi::disableResourceLoading() {
    std::lock_guard<std::mutex> lock(s_resource_loading_mutex);

    if (s_resource_loading_state != ResourceLoadingState::Uninitialized) {
        ALOGD("ResourceLoading: Returning cached state");
        return (s_resource_loading_state == ResourceLoadingState::Available) ? JNI_TRUE : JNI_FALSE;
    }

    char version_str[PROP_VALUE_MAX];
    long android_version = 0;
    if (__system_property_get("ro.build.version.sdk", version_str)) {
        android_version = std::strtol(version_str, nullptr, 10);
    }
    ALOGD("ResourceLoading: Android API = %ld", android_version);

    s_resource_loading_state = ResourceLoadingState::Resolving;

    ALOGD("ResourceLoading: Attempting strategy = ApkAssetsNativeLoad");

    // Hold a static handle to prevent library unloading and ensure symbol pointer validity
    if (!s_libandroid_runtime_handle) {
        s_libandroid_runtime_handle = shadowhook_dlopen("libandroid_runtime.so");
    }

    void* nativeLoadAddr = nullptr;

    if (s_libandroid_runtime_handle) {
        // Strategy A: Direct JNI exported symbol
        nativeLoadAddr = shadowhook_dlsym(s_libandroid_runtime_handle, "Java_android_content_res_ApkAssets_nativeLoad");
        if (nativeLoadAddr) {
            ALOGD("ResourceLoading: Compatible implementation resolved via Java_android_content_res_ApkAssets_nativeLoad at %p", nativeLoadAddr);
            s_resource_loading_state = ResourceLoadingState::Available;
        } else {
            // Strategy B: Standard mangled symbol on older platforms
            nativeLoadAddr = shadowhook_dlsym(s_libandroid_runtime_handle, "_ZN7android8ApkAssets9nativeLoadEPKc");
            if (nativeLoadAddr) {
                ALOGD("ResourceLoading: Compatible implementation resolved via _ZN7android8ApkAssets9nativeLoadEPKc at %p", nativeLoadAddr);
                s_resource_loading_state = ResourceLoadingState::Available;
            } else {
                s_resource_loading_state = ResourceLoadingState::Unsupported;
            }
        }
    } else {
        s_resource_loading_state = ResourceLoadingState::Failed;
    }

    if (s_resource_loading_state == ResourceLoadingState::Available) {
        ALOGD("ResourceLoading: State = Available");
        return JNI_TRUE;
    }

    if (s_resource_loading_state == ResourceLoadingState::Failed) {
        ALOGE("ResourceLoading: State = Failed");
        ALOGE("Reason = libandroid_runtime.so could not be opened");
        return JNI_FALSE;
    }

    if (s_resource_loading_state == ResourceLoadingState::Unsupported) {
        // Unresolved: Clean up opened handle to prevent resource leaks
        if (s_libandroid_runtime_handle) {
            shadowhook_dlclose(s_libandroid_runtime_handle);
            s_libandroid_runtime_handle = nullptr;
        }
        ALOGD("ResourceLoading: State = Unsupported");
        ALOGD("Reason = no compatible ApkAssets.nativeLoad target found");
        return JNI_FALSE;
    }

    return JNI_FALSE;
}

} // namespace blackbox
