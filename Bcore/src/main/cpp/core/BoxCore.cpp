#include "core/BoxCore.h"
#include "utils/Log.h"
#include <jni.h>
#include "hooks/jni/JniHook.h"
#include "hooks/runtime/VMClassLoaderHook.h"
#include "hooks/filesystem/UnixFileSystemHook.h"
#include "hooks/filesystem/FileSystemHook.h"
#include "hooks/runtime/BinderHook.h"
#include "hooks/runtime/DexFileHook.h"

#include <cstdint>
#include <string>
#include <mutex>
#include <condition_variable>

namespace blackbox {

static std::mutex sInitMutex;
static std::condition_variable sInitCondVar;

// Initialize static members of BoxCore
JavaVM *BoxCore::vm = nullptr;
jclass BoxCore::NativeCoreClass = nullptr;
jmethodID BoxCore::getCallingUidId = nullptr;
jmethodID BoxCore::redirectPathStringId = nullptr;
jmethodID BoxCore::redirectPathFileId = nullptr;
jmethodID BoxCore::loadEmptyDexId = nullptr;
jmethodID BoxCore::loadEmptyDexLId = nullptr;
int BoxCore::api_level = 0;

std::atomic<InitState> BoxCore::sState{InitState::Uninitialized};
FeatureHealth BoxCore::sHealth{false, false, false, false, false, false, NativeError::Ok};

int AndroidRuntimeInfo::sdkInt() {
    return BoxCore::api_level;
}

bool AndroidRuntimeInfo::isAtLeast(int version) {
    return BoxCore::api_level >= version;
}

bool BoxCore::isReady() {
    return sState.load() == InitState::Ready;
}

FeatureHealth BoxCore::getHealth() {
    return sHealth;
}

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
    if (sState.load() != InitState::Ready || !BoxCore::NativeCoreClass || !BoxCore::getCallingUidId) {
        ALOGW("getCallingUid called when BoxCore is not Ready! State: %d. Returning original value.", (int)sState.load());
        return orig;
    }
    env = BoxCore::ensureEnvCreated();
    return env->CallStaticIntMethod(BoxCore::NativeCoreClass, BoxCore::getCallingUidId, orig);
}

jstring BoxCore::redirectPathString(JNIEnv *env, jstring path) {
    if (sState.load() != InitState::Ready || !BoxCore::NativeCoreClass || !BoxCore::redirectPathStringId) {
        ALOGW("redirectPathString called when BoxCore is not Ready! State: %d. Returning original path.", (int)sState.load());
        return path;
    }
    env = BoxCore::ensureEnvCreated();
    return (jstring) env->CallStaticObjectMethod(BoxCore::NativeCoreClass, BoxCore::redirectPathStringId, path);
}

jobject BoxCore::redirectPathFile(JNIEnv *env, jobject path) {
    if (sState.load() != InitState::Ready || !BoxCore::NativeCoreClass || !BoxCore::redirectPathFileId) {
        ALOGW("redirectPathFile called when BoxCore is not Ready! State: %d. Returning original path.", (int)sState.load());
        return path;
    }
    env = BoxCore::ensureEnvCreated();
    return env->CallStaticObjectMethod(BoxCore::NativeCoreClass, BoxCore::redirectPathFileId, path);
}

jlongArray BoxCore::loadEmptyDex(JNIEnv *env) {
    if (sState.load() != InitState::Ready || !BoxCore::NativeCoreClass || !BoxCore::loadEmptyDexId) {
        ALOGW("loadEmptyDex called when BoxCore is not Ready! State: %d. Returning nullptr.", (int)sState.load());
        return nullptr;
    }
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
    std::unique_lock<std::mutex> lock(sInitMutex);
    
    InitState currentState = sState.load();
    if (currentState == InitState::Shutdown || currentState == InitState::ShuttingDown) {
        ALOGE("BoxCore reinitialization is NOT SUPPORTED after shutdown.");
        sHealth.lastError = NativeError::AlreadyInitialized;
        return;
    }
    
    // Check if another thread is currently initializing
    if (currentState == InitState::Initializing) {
        ALOGD("BoxCore is currently initializing. Thread waiting for completion...");
        sInitCondVar.wait(lock, []() {
            return sState.load() != InitState::Initializing;
        });
        ALOGD("BoxCore initialization wait complete. Final state: %d", (int)sState.load());
        return;
    }
    
    // Check if already ready
    if (sState.load() == InitState::Ready) {
        ALOGD("BoxCore is already Ready.");
        return;
    }
    
    // Check if failed previously
    if (sState.load() == InitState::Failed) {
        ALOGW("BoxCore previously failed initialization. Attempting retry...");
    }

    sState.store(InitState::Initializing);
    sHealth.coreReady = false; // coreReady must remain false while Initializing
    sHealth.jniReady = true;
    sHealth.lastError = NativeError::Ok;
    
    // Unlock while performing the actual hook installation to avoid holding the lock
    // for long-running synchronous operations, allowing concurrent threads to wait.
    lock.unlock();

    bool success = true;

    try {
        BaseHook::init(env);
    } catch (...) {
        ALOGE("Failed to initialize BaseHook");
        sHealth.lastError = NativeError::HookInstallFailure;
        success = false;
    }

    try {
        UnixFileSystemHook::init(env);
        sHealth.ioReady = true;
    } catch (...) {
        ALOGE("Failed to initialize UnixFileSystemHook");
        sHealth.ioReady = false;
        sHealth.lastError = NativeError::HookInstallFailure;
        success = false;
    }

    try {
        FileSystemHook::init();
    } catch (...) {
        ALOGE("Failed to initialize FileSystemHook");
        // Non-critical, but logging warning
    }

    try {
        VMClassLoaderHook::init(env);
        sHealth.classLoaderReady = true;
    } catch (...) {
        ALOGE("Failed to initialize VMClassLoaderHook");
        sHealth.classLoaderReady = false;
        sHealth.lastError = NativeError::HookInstallFailure;
        success = false;
    }

    try {
        BinderHook::init(env);
        sHealth.binderReady = true;
    } catch (...) {
        ALOGE("Failed to initialize BinderHook");
        sHealth.binderReady = false;
        sHealth.lastError = NativeError::HookInstallFailure;
        success = false;
    }

    try {
        DexFileHook::init(env);
        sHealth.dexReady = true;
    } catch (...) {
        ALOGE("Failed to initialize DexFileHook");
        sHealth.dexReady = false;
        sHealth.lastError = NativeError::HookInstallFailure;
        success = false;
    }

    // Re-acquire lock to transition final state
    lock.lock();
    if (success) {
        sState.store(InitState::Ready);
        sHealth.coreReady = true; // Set true ONLY when fully and successfully ready
        ALOGD("BoxCore initialization completed successfully! Ready state stored.");
    } else {
        sState.store(InitState::Failed);
        sHealth.coreReady = false;
        ALOGE("BoxCore initialization failed! Failed state stored.");
    }
    
    // Notify all waiting threads of the state change
    sInitCondVar.notify_all();
}

void BoxCore::shutdown(JNIEnv *env) {
    InitState expected = InitState::Ready;
    if (!sState.compare_exchange_strong(expected, InitState::ShuttingDown)) {
        expected = InitState::Initializing;
        if (!sState.compare_exchange_strong(expected, InitState::ShuttingDown)) {
            ALOGW("BoxCore not in Ready or Initializing state for shutdown.");
            return;
        }
    }

    ALOGD("Performing LOGICAL SHUTDOWN of BoxCore...");

    /*
     * LOGICAL SHUTDOWN SEMANTICS:
     * Resources released:
     * - NativeCore GlobalRefs (NativeCoreClass deleted via DeleteGlobalRef)
     * - JniHook global references (method_utils_class deleted via DeleteGlobalRef)
     * - Cached JNI method IDs set to nullptr
     * - sHealth.coreReady = false, sHealth.jniReady = false
     * 
     * Resources intentionally remaining active for process lifetime:
     * - ShadowHook hooks (FileSystemHook, UnixFileSystemHook, readlink, etc.)
     * - ART swaps (VMClassLoaderHook, DexFileHook, BinderHook, JniHook)
     * - Process-lifetime original function pointers
     * 
     * Therefore, physical instrumentation status flags (ioReady, binderReady, dexReady, classLoaderReady)
     * are intentionally left as-is (active) to accurately report that they are NOT physically uninstalled.
     */

    // Clean up JniHook GlobalRefs
    JniHook::DeinitJniHook(env);

    // Clean up BoxCore GlobalRef
    if (env && NativeCoreClass) {
        env->DeleteGlobalRef(NativeCoreClass);
        NativeCoreClass = nullptr;
    }

    getCallingUidId = nullptr;
    redirectPathStringId = nullptr;
    redirectPathFileId = nullptr;
    loadEmptyDexId = nullptr;
    loadEmptyDexLId = nullptr;

    sHealth.coreReady = false;
    sHealth.jniReady = false;
    // ioReady, binderReady, dexReady, and classLoaderReady intentionally remain true as physical hooks remain in memory

    sState.store(InitState::Shutdown);
    ALOGD("BoxCore logical shutdown completed successfully.");
}

} // namespace blackbox
