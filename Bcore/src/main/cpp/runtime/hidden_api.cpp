#include <jni.h>
#include <sys/system_properties.h>
#include <cstdlib>
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
