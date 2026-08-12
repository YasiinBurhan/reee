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
std::atomic<int> BoxCore::api_level = 0;
std::atomic<InitState> BoxCore::initState = InitState::Uninitialized;
FeatureHealth BoxCore::health{};
std::mutex BoxCore::init_mutex;
std::condition_variable BoxCore::init_cv;

JNIEnv *BoxCore::getEnv() {
    JNIEnv *env = nullptr;
    if (BoxCore::vm == nullptr) return nullptr;
    BoxCore::vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    return env;
}

JNIEnv *BoxCore::ensureEnvCreated() {
    JNIEnv *env = BoxCore::getEnv();
    if (env == nullptr) {
        if (BoxCore::vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return nullptr;
        }
        // Note: In a production refactor, we should use RAII to detach if we attached.
        // But for global core methods, many threads might call this.
    }
    return env;
}

int BoxCore::getCallingUid(JNIEnv *env, int orig) {
    env = BoxCore::ensureEnvCreated();
    if (env == nullptr || BoxCore::NativeCoreClass == nullptr || BoxCore::getCallingUidId == nullptr) return orig;
    return env->CallStaticIntMethod(BoxCore::NativeCoreClass, BoxCore::getCallingUidId, orig);
}

jstring BoxCore::redirectPathString(JNIEnv *env, jstring path) {
    env = BoxCore::ensureEnvCreated();
    if (env == nullptr || BoxCore::NativeCoreClass == nullptr || BoxCore::redirectPathStringId == nullptr) return path;
    return (jstring) env->CallStaticObjectMethod(BoxCore::NativeCoreClass, BoxCore::redirectPathStringId, path);
}

jobject BoxCore::redirectPathFile(JNIEnv *env, jobject path) {
    env = BoxCore::ensureEnvCreated();
    if (env == nullptr || BoxCore::NativeCoreClass == nullptr || BoxCore::redirectPathFileId == nullptr) return path;
    return env->CallStaticObjectMethod(BoxCore::NativeCoreClass, BoxCore::redirectPathFileId, path);
}

jlongArray BoxCore::loadEmptyDex(JNIEnv *env) {
    env = BoxCore::ensureEnvCreated();
    if (env == nullptr || BoxCore::NativeCoreClass == nullptr || BoxCore::loadEmptyDexId == nullptr) return nullptr;
    return (jlongArray) env->CallStaticObjectMethod(BoxCore::NativeCoreClass, BoxCore::loadEmptyDexId);
}

int BoxCore::getApiLevel() {
    return BoxCore::api_level.load();
}

InitState BoxCore::getInitState() {
    return BoxCore::initState.load();
}

FeatureHealth BoxCore::getHealth() {
    std::lock_guard<std::mutex> lock(BoxCore::init_mutex);
    return BoxCore::health;
}

JavaVM *BoxCore::getJavaVM() {
    return BoxCore::vm;
}

void BoxCore::nativeHook(JNIEnv *env) {
    std::unique_lock<std::mutex> lock(BoxCore::init_mutex);
    InitState current = BoxCore::initState.load();
    if (current == InitState::Ready) {
        return;
    }

    // Option B Contract: Reinitialization after shutdown is NOT supported.
    if (current == InitState::ShuttingDown || current == InitState::Shutdown) {
        BoxCore::health.lastError = NativeError::INITIALIZATION_FAILED;
        return;
    }
    
    // Concurrent Initialization Behavior:
    // If another thread is currently initializing, wait for it to finish.
    if (current == InitState::Initializing) {
        BoxCore::init_cv.wait(lock, []() {
            InitState s = BoxCore::initState.load();
            return s != InitState::Initializing;
        });
        return;
    }

    // Priority 1: Set Initializing state. coreReady remains FALSE.
    BoxCore::initState.store(InitState::Initializing);
    BoxCore::health.coreReady = false;

    try {
        BaseHook::init(env);
        BoxCore::health.jni.status = SubsystemStatus::Ready;

        UnixFileSystemHook::init(env);
        FileSystemHook::init();
        BoxCore::health.io.status = SubsystemStatus::Ready;
        BoxCore::health.fileSystemHook.status = SubsystemStatus::Ready;

        VMClassLoaderHook::init(env);
        BoxCore::health.vmClassLoaderHook.status = SubsystemStatus::Ready;

        BinderHook::init(env);
        BoxCore::health.binderHook.status = SubsystemStatus::Ready;

        DexFileHook::init(env);
        BoxCore::health.dexFileHook.status = SubsystemStatus::Ready;

        BoxCore::health.lastError = NativeError::OK;
        
        // Priority 1: coreReady is only set to true AFTER all steps succeed and state is Ready.
        BoxCore::health.coreReady = true;
        BoxCore::initState.store(InitState::Ready);
        BoxCore::init_cv.notify_all();
    } catch (...) {
        BoxCore::health.coreReady = false;
        BoxCore::health.lastError = NativeError::INITIALIZATION_FAILED;
        BoxCore::initState.store(InitState::Failed);
        BoxCore::init_cv.notify_all();
    }
}

void BoxCore::shutdown(JNIEnv *env) {
    std::unique_lock<std::mutex> lock(BoxCore::init_mutex);
    BoxCore::initState.store(InitState::ShuttingDown);
    
    if (env != nullptr && BoxCore::NativeCoreClass != nullptr) {
        env->DeleteGlobalRef(BoxCore::NativeCoreClass);
        BoxCore::NativeCoreClass = nullptr;
    }
    
    BoxCore::health.coreReady = false;
    BoxCore::initState.store(InitState::Shutdown);
    BoxCore::init_cv.notify_all();
}

} // namespace blackbox
