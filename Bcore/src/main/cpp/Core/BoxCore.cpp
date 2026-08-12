#include "BoxCore.h"
#include "Log.h"
#include <jni.h>
#include <JniHook/JniHook.h>
#include <Hook/VMClassLoaderHook.h>
#include <Hook/UnixFileSystemHook.h>
#include <Hook/FileSystemHook.h>
#include <Hook/BinderHook.h>
#include <Hook/DexFileHook.h>

#include <cstdint>
#include <string>

namespace blackbox {

// Initialize static members of BoxCore
JavaVM *BoxCore::vm = nullptr;
jclass BoxCore::NativeCoreClass = nullptr;
jmethodID BoxCore::getCallingUidId = nullptr;
jmethodID BoxCore::redirectPathStringId = nullptr;
jmethodID BoxCore::redirectPathFileId = nullptr;
jmethodID BoxCore::loadEmptyDexId = nullptr;
jmethodID BoxCore::loadEmptyDexLId = nullptr;
int BoxCore::api_level = 0;

JNIEnv *BoxCore::getEnv() {
    JNIEnv *env = nullptr;
    BoxCore::vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    return env;
}

JNIEnv *BoxCore::ensureEnvCreated() {
    JNIEnv *env = BoxCore::getEnv();
    if (env == nullptr) {
        BoxCore::vm->AttachCurrentThread(&env, nullptr);
    }
    return env;
}

int BoxCore::getCallingUid(JNIEnv *env, int orig) {
    env = BoxCore::ensureEnvCreated();
    return env->CallStaticIntMethod(BoxCore::NativeCoreClass, BoxCore::getCallingUidId, orig);
}

jstring BoxCore::redirectPathString(JNIEnv *env, jstring path) {
    env = BoxCore::ensureEnvCreated();
    return (jstring) env->CallStaticObjectMethod(BoxCore::NativeCoreClass, BoxCore::redirectPathStringId, path);
}

jobject BoxCore::redirectPathFile(JNIEnv *env, jobject path) {
    env = BoxCore::ensureEnvCreated();
    return env->CallStaticObjectMethod(BoxCore::NativeCoreClass, BoxCore::redirectPathFileId, path);
}

jlongArray BoxCore::loadEmptyDex(JNIEnv *env) {
    env = BoxCore::ensureEnvCreated();
    return (jlongArray) env->CallStaticObjectMethod(BoxCore::NativeCoreClass, BoxCore::loadEmptyDexId);
}

int BoxCore::getApiLevel() {
    return BoxCore::api_level;
}

JavaVM *BoxCore::getJavaVM() {
    return BoxCore::vm;
}

void BoxCore::nativeHook(JNIEnv *env) {
    BaseHook::init(env);
    UnixFileSystemHook::init(env);
    FileSystemHook::init();
    VMClassLoaderHook::init(env);

    BinderHook::init(env);
    DexFileHook::init(env);
}

} // namespace blackbox
