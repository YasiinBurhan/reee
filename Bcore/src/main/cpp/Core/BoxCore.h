#ifndef BLACKBOX_BOXCORE_H
#define BLACKBOX_BOXCORE_H

#include <jni.h>
#include <atomic>

#define VMCORE_CLASS "com/equinox/virtual/core/NativeCore"

namespace blackbox {

enum class InitState {
    Uninitialized,
    Initializing,
    Ready,
    Failed,
    ShuttingDown,
    Shutdown
};

enum class NativeError {
    Ok = 0,
    InvalidArgument,
    NotInitialized,
    AlreadyInitialized,
    JniFailure,
    JniException,
    LibraryLoadFailure,
    SymbolNotFound,
    HookInstallFailure,
    HookUninstallFailure,
    UnsupportedAndroidVersion,
    UnsupportedArchitecture,
    MemoryProtectionFailure,
    MemoryWriteFailure,
    InternalError
};

struct FeatureHealth {
    bool coreReady;
    bool jniReady;
    bool ioReady;
    bool binderReady;
    bool dexReady;
    bool classLoaderReady;
    NativeError lastError;
};

class AndroidRuntimeInfo {
public:
    static int sdkInt();
    static bool isAtLeast(int version);
};

class BoxCore {
public:
    static JavaVM *vm;
    static jclass NativeCoreClass;
    static jmethodID getCallingUidId;
    static jmethodID redirectPathStringId;
    static jmethodID redirectPathFileId;
    static jmethodID loadEmptyDexId;
    static jmethodID loadEmptyDexLId;
    static int api_level;
    
    static std::atomic<InitState> sState;
    static FeatureHealth sHealth;

    static JNIEnv *getEnv();
    static JNIEnv *ensureEnvCreated();
    static void nativeHook(JNIEnv *env);
    static void shutdown(JNIEnv *env);

    static JavaVM *getJavaVM();
    static int getApiLevel();
    static int getCallingUid(JNIEnv *env, int orig);
    static jstring redirectPathString(JNIEnv *env, jstring path);
    static jobject redirectPathFile(JNIEnv *env, jobject path);
    static jlongArray loadEmptyDex(JNIEnv *env);
    
    static bool isReady();
    static FeatureHealth getHealth();
};

} // namespace blackbox

#endif // BLACKBOX_BOXCORE_H
