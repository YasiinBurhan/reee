#include <Core/BoxCore.h>
#include "Log.h"
#include <Base/IO.h>
#include <jni.h>
#include <JniHook/JniHook.h>
#include <Hook/VMClassLoaderHook.h>
#include <Hook/ImGuiHook.h>
#include <Base/hidden_api.h>
#include <string>

extern "C" {

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_initMenuModSurfaceHook(JNIEnv *env, jclass clazz, jstring package_name) {
    const char *pkg = env->GetStringUTFChars(package_name, JNI_FALSE);
    ALOGD("Initializing MenuMod surface hook for: %s", pkg);
    blackbox::ImGuiHook::init(pkg);
    env->ReleaseStringUTFChars(package_name, pkg);
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
    blackbox::BoxCore::NativeCoreClass = (jclass) env->NewGlobalRef(env->FindClass(VMCORE_CLASS));
    blackbox::BoxCore::getCallingUidId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "getCallingUid", "(I)I");
    blackbox::BoxCore::redirectPathStringId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "redirectPath",
                                                      "(Ljava/lang/String;)Ljava/lang/String;");
    blackbox::BoxCore::redirectPathFileId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "redirectPath",
                                                    "(Ljava/io/File;)Ljava/io/File;");
    blackbox::BoxCore::loadEmptyDexId = env->GetStaticMethodID(blackbox::BoxCore::NativeCoreClass, "loadEmptyDex",
                                                "()[J");

    blackbox::JniHook::InitJniHook(env, api_level);
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_addIORule(JNIEnv *env, jclass clazz, jstring target_path,
                                                   jstring relocate_path) {
    ALOGD("set addIORule");
    blackbox::IO::addRule(env->GetStringUTFChars(target_path, JNI_FALSE),
                env->GetStringUTFChars(relocate_path, JNI_FALSE));
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

} // extern "C"

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    blackbox::BoxCore::vm = vm;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_EVERSION;
    }
    return JNI_VERSION_1_6;
}
