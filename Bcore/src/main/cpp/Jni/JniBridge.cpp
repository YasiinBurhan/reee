#include "BoxCore.h"
#include "Log.h"
#include "IO.h"
#include <jni.h>
#include <JniHook/JniHook.h>
#include <Hook/VMClassLoaderHook.h>
#include <Hook/ImGuiHook.h>
#include "hidden_api.h"

#include <string>

extern "C" {

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_initImGuiSurfaceHook(JNIEnv *env, jclass clazz, jstring package_name) {
    const char *pkg = env->GetStringUTFChars(package_name, JNI_FALSE);
    ALOGD("Initializing ImGui surface hook for: %s", pkg);
    ImGuiHook::init(pkg);
    env->ReleaseStringUTFChars(package_name, pkg);
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_setImGuiHookEnabled(JNIEnv *env, jclass clazz, jboolean enabled) {
    ALOGD("Setting ImGui surface hook enabled: %d", enabled);
    ImGuiHook::setEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_hideXposed(JNIEnv *env, jclass clazz) {
    ALOGD("set hideXposed");
    VMClassLoaderHook::hideXposed();
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_init(JNIEnv *env, jclass clazz, jint api_level) {
    ALOGD("NativeCore init.");
    BoxCore::api_level = api_level;
    BoxCore::NativeCoreClass = (jclass) env->NewGlobalRef(env->FindClass(VMCORE_CLASS));
    BoxCore::getCallingUidId = env->GetStaticMethodID(BoxCore::NativeCoreClass, "getCallingUid", "(I)I");
    BoxCore::redirectPathStringId = env->GetStaticMethodID(BoxCore::NativeCoreClass, "redirectPath",
                                                      "(Ljava/lang/String;)Ljava/lang/String;");
    BoxCore::redirectPathFileId = env->GetStaticMethodID(BoxCore::NativeCoreClass, "redirectPath",
                                                    "(Ljava/io/File;)Ljava/io/File;");
    BoxCore::loadEmptyDexId = env->GetStaticMethodID(BoxCore::NativeCoreClass, "loadEmptyDex",
                                                "()[J");

    JniHook::InitJniHook(env, api_level);
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_addIORule(JNIEnv *env, jclass clazz, jstring target_path,
                                                   jstring relocate_path) {
    ALOGD("set addIORule");
    IO::addRule(env->GetStringUTFChars(target_path, JNI_FALSE),
                env->GetStringUTFChars(relocate_path, JNI_FALSE));
}

JNIEXPORT void JNICALL
Java_com_equinox_virtual_core_NativeCore_enableIO(JNIEnv *env, jclass clazz) {
    ALOGD("set enableIO");
    IO::init(env);
    BoxCore::nativeHook(env);
}

JNIEXPORT jboolean JNICALL
Java_com_equinox_virtual_core_NativeCore_disableHiddenApi(JNIEnv *env, jclass clazz) {
    ALOGD("set disableHiddenApi");
    if(!disable_hidden_api(env)){
        ALOGD("set disableHiddenApi Fail!!!");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_equinox_virtual_core_NativeCore_disableResourceLoading(JNIEnv *env, jclass clazz) {
    ALOGD("set disableResourceLoading");
    if(!disable_resource_loading()){
        ALOGD("set disableResourceLoading Fail!!!");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

} // extern "C"

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    BoxCore::vm = vm;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_EVERSION;
    }
    return JNI_VERSION_1_6;
}

