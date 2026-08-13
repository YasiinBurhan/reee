#include <Core/BoxCore.h>
#include "Log.h"
#include <Base/IO.h>
#include <Base/JniUtils.h>
#include <jni.h>
#include <JniHook/JniHook.h>
#include <Hook/VMClassLoaderHook.h>
#include <Hook/ImGuiHook.h>
#include <Base/hidden_api.h>
#include <string>
#include <Core/Diagnostics.h>

extern "C" {

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_initMenuModSurfaceHook(JNIEnv *env, jclass clazz, jstring package_name) {
    blackbox::ScopedUtfChars pkg(env, package_name);
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
    ALOGD("NativeCore init.");
    blackbox::BoxCore::api_level = api_level;
    
    jclass localClass = env->FindClass(VMCORE_CLASS);
    if (!localClass) {
        ALOGE("Failed to find class: %s", VMCORE_CLASS);
        env->ExceptionClear();
        return;
    }
    
    blackbox::BoxCore::NativeCoreClass = (jclass) env->NewGlobalRef(localClass);
    env->DeleteLocalRef(localClass);

    blackbox::BoxCore::getCallingUidId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "getCallingUid", "(I)I");
    if (!blackbox::BoxCore::getCallingUidId) {
        ALOGE("Failed to find static method getCallingUid");
        env->ExceptionClear();
    }

    blackbox::BoxCore::redirectPathStringId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "redirectPath",
                                                      "(Ljava/lang/String;)Ljava/lang/String;");
    if (!blackbox::BoxCore::redirectPathStringId) {
        ALOGE("Failed to find static method redirectPath(String)");
        env->ExceptionClear();
    }

    blackbox::BoxCore::redirectPathFileId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "redirectPath",
                                                    "(Ljava/io/File;)Ljava/io/File;");
    if (!blackbox::BoxCore::redirectPathFileId) {
        ALOGE("Failed to find static method redirectPath(File)");
        env->ExceptionClear();
    }

    blackbox::BoxCore::loadEmptyDexId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "loadEmptyDex",
                                                "()[J");
    if (!blackbox::BoxCore::loadEmptyDexId) {
        ALOGE("Failed to find static method loadEmptyDex");
        env->ExceptionClear();
    }

    blackbox::JniHook::InitJniHook(env, api_level);
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_addIORule(JNIEnv *env, jclass clazz, jstring target_path,
                                                   jstring relocate_path) {
    ALOGD("set addIORule");
    blackbox::ScopedUtfChars target(env, target_path);
    blackbox::ScopedUtfChars relocate(env, relocate_path);
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

JNIEXPORT jstring JNICALL
Java_com_equinox_virtual_core_NativeCore_runDiagnosticsTest(JNIEnv *env, jclass clazz) {
#if BCORE_DIAGNOSTICS
    std::string result = blackbox::runDiagnosticsTest();
    return env->NewStringUTF(result.c_str());
#else
    return env->NewStringUTF("Diagnostics disabled");
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
    }
}
