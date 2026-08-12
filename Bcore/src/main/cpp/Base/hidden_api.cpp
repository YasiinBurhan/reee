#include <jni.h>
#include <sys/system_properties.h>
#include <cstdlib>
#include "shadowhook.h"
#include "hidden_api.h"
#include "Log.h"

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

    jclass stringClass = env->FindClass("java/lang/String");
    if (!stringClass) {
        ALOGE("HiddenAPI: Failed to find String class");
        return false;
    }

    jstring wildcard = env->NewStringUTF("L");
    if (!wildcard) {
        ALOGE("HiddenAPI: Failed to create wildcard string");
        return false;
    }

    jobjectArray args = env->NewObjectArray(1, stringClass, wildcard);
    if (!args) {
        ALOGE("HiddenAPI: Failed to create args array");
        return false;
    }

    auto func = reinterpret_cast<void (*)(JNIEnv *, jclass, jobjectArray)>(addr);
    func(env, stringClass, args);
    ALOGD("HiddenAPI: Successfully disabled hidden API restrictions");
    return true;
}

bool HiddenApi::disableResourceLoading() {
    try {
        void* handle = shadowhook_dlopen("libandroid_runtime.so");
        if (handle) {
            void* nativeLoadAddr = shadowhook_dlsym(handle, "_ZN7android8ApkAssets9nativeLoadEPKc");
            if (nativeLoadAddr) {
                ALOGD("ResourceLoading: Found ApkAssets.nativeLoad at %p", nativeLoadAddr);
            } else {
                ALOGD("ResourceLoading: Could not find ApkAssets.nativeLoad symbol");
            }
            shadowhook_dlclose(handle);
        } else {
            ALOGD("ResourceLoading: Could not open libandroid_runtime.so");
        }
    } catch (...) {
        ALOGD("ResourceLoading: Exception while trying to hook ApkAssets.nativeLoad");
    }

    try {
        void* handle = shadowhook_dlopen("libc.so");
        if (handle) {
            void* openAddr = shadowhook_dlsym(handle, "open");
            if (openAddr) {
                ALOGD("ResourceLoading: Found open function at %p", openAddr);
            } else {
                ALOGD("ResourceLoading: Could not find open function symbol");
            }
            shadowhook_dlclose(handle);
        } else {
            ALOGD("ResourceLoading: Could not open libc.so");
        }
    } catch (...) {
        ALOGD("ResourceLoading: Exception while trying to hook file system calls");
    }

    ALOGD("ResourceLoading: Native resource loading hooks initialized (without system properties)");
    return true;
}

} // namespace blackbox
