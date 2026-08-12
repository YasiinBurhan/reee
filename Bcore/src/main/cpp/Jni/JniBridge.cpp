#include <Core/BoxCore.h>
#include "Log.h"
#include <Base/IO.h>
#include <jni.h>
#include <JniHook/JniHook.h>
#include <Hook/VMClassLoaderHook.h>
#include <Hook/ImGuiHook.h>
#include <Base/hidden_api.h>
#include <Utils/SafeJni.h>
#include <Utils/TestHarness.h>
#include <string>

extern "C" {

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_initMenuModSurfaceHook(JNIEnv *env, jclass clazz, jstring package_name) {
    if (package_name == nullptr) return;
    blackbox::ScopedUtfChars pkg(env, package_name);
    if (pkg.c_str() == nullptr) return;
    
    ALOGD("Initializing MenuMod surface hook for: %s", pkg.c_str());
    blackbox::ImGuiHook::init(pkg.c_str());
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_setMenuModHookEnabled(JNIEnv *env, jclass clazz, jboolean enabled) {
    ALOGD("Setting MenuMod surface hook enabled: %d", enabled);
    blackbox::ImGuiHook::setEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_hideXposed(JNIEnv *env, jclass clazz) {
    ALOGD("set hideXposed");
    blackbox::VMClassLoaderHook::hideXposed();
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_init(JNIEnv *env, jclass clazz, jint api_level) {
    ALOGD("NativeCore init. API Level: %d", api_level);
    blackbox::BoxCore::api_level = api_level;
    
    jclass nativeCoreClass = env->FindClass(VMCORE_CLASS);
    if (nativeCoreClass == nullptr) {
        ALOGE("Failed to find NativeCore class: %s", VMCORE_CLASS);
        return;
    }
    
    // Clean up old global ref if any (though init should only be called once)
    if (blackbox::BoxCore::NativeCoreClass != nullptr) {
        env->DeleteGlobalRef(blackbox::BoxCore::NativeCoreClass);
    }
    
    blackbox::BoxCore::NativeCoreClass = (jclass) env->NewGlobalRef(nativeCoreClass);
    blackbox::BoxCore::getCallingUidId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "getCallingUid", "(I)I");
    blackbox::BoxCore::redirectPathStringId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "redirectPath",
                                                      "(Ljava/lang/String;)Ljava/lang/String;");
    blackbox::BoxCore::redirectPathFileId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "redirectPath",
                                                    "(Ljava/io/File;)Ljava/io/File;");
    blackbox::BoxCore::loadEmptyDexId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "loadEmptyDex",
                                                "()[J");

    if (blackbox::BoxCore::getCallingUidId == nullptr || 
        blackbox::BoxCore::redirectPathStringId == nullptr ||
        blackbox::BoxCore::redirectPathFileId == nullptr ||
        blackbox::BoxCore::loadEmptyDexId == nullptr) {
        ALOGE("Failed to find some critical NativeCore methods");
    }

    blackbox::JniHook::InitJniHook(env, api_level);
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_addIORule(JNIEnv *env, jclass clazz, jstring target_path,
                                                   jstring relocate_path) {
    if (target_path == nullptr || relocate_path == nullptr) return;
    
    blackbox::ScopedUtfChars target(env, target_path);
    blackbox::ScopedUtfChars relocate(env, relocate_path);
    
    if (target.c_str() == nullptr || relocate.c_str() == nullptr) return;
    
    ALOGD("addIORule: %s -> %s", target.c_str(), relocate.c_str());
    blackbox::IO::addRule(target.c_str(), relocate.c_str());
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_enableIO(JNIEnv *env, jclass clazz) {
    ALOGD("set enableIO");
    blackbox::IO::init(env);
    blackbox::BoxCore::nativeHook(env);
}

JNIEXPORT jboolean JNICALL
Java_com_equinox_virtual_core_NativeCore_disableHiddenApi(JNIEnv *env, jclass clazz) {
    ALOGD("set disableHiddenApi");
    if (!blackbox::HiddenApi::disableHiddenApi(env)) {
        ALOGD("set disableHiddenApi Fail!!!");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_equinox_virtual_core_NativeCore_disableResourceLoading(JNIEnv *env, jclass clazz) {
    ALOGD("set disableResourceLoading");
    if (!blackbox::HiddenApi::disableResourceLoading()) {
        ALOGD("set disableResourceLoading Fail!!!");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jobjectArray JNICALL
Java_com_equinox_virtual_core_NativeCore_runDiagnosticsTest(JNIEnv *env, jclass clazz) {
#if BCORE_DIAGNOSTICS
    auto results = blackbox::TestHarness::runAllDiagnostics(env);
    jclass stringClass = env->FindClass("java/lang/String");
    if (stringClass == nullptr) return nullptr;
    
    jobjectArray array = env->NewObjectArray(static_cast<jsize>(results.size()), stringClass, nullptr);
    for (size_t i = 0; i < results.size(); ++i) {
        std::string line = "[" + results[i].category + "] " + results[i].name + 
                           " | Static: " + results[i].staticStatus + 
                           " | Runtime: " + results[i].runtimeStatus + 
                           " | Final: " + results[i].finalResult + 
                           " - " + results[i].message;
        jstring str = env->NewStringUTF(line.c_str());
        env->SetObjectArrayElement(array, static_cast<jsize>(i), str);
        env->DeleteLocalRef(str);
    }
    return array;
#else
    jclass stringClass = env->FindClass("java/lang/String");
    if (stringClass == nullptr) return nullptr;
    jobjectArray array = env->NewObjectArray(1, stringClass, nullptr);
    jstring str = env->NewStringUTF("[Diagnostics] Disabled in release build");
    env->SetObjectArrayElement(array, 0, str);
    env->DeleteLocalRef(str);
    return array;
#endif
}

} // extern "C"

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    blackbox::BoxCore::vm = vm;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_EVERSION;
    }
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) == JNI_OK) {
        blackbox::BoxCore::shutdown(env);
        blackbox::JniHook::shutdown(env);
    }
}
