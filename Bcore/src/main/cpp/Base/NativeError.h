#ifndef BCORE_NATIVEERROR_H
#define BCORE_NATIVEERROR_H

namespace blackbox {

enum class NativeError : int {
    OK = 0,
    UNKNOWN = -1,
    INVALID_ARGUMENT = -2,
    INITIALIZATION_FAILED = -3,
    HOOK_FAILED = -4,
    MEMORY_ERROR = -5,
    JNI_ERROR = -6,
    NOT_SUPPORTED = -7,
    RESOURCE_BUSY = -8,
    NOT_FOUND = -9,
    PERMISSION_DENIED = -10
};

enum class InitState : int {
    Uninitialized = 0,
    Initializing = 1,
    Ready = 2,
    Failed = 3,
    ShuttingDown = 4,
    Shutdown = 5
};

enum class SubsystemStatus : int {
    NotInitialized = 0,
    Initializing = 1,
    Ready = 2,
    Partial = 3,
    Failed = 4,
    Disabled = 5,
    Unsupported = 6
};

struct SubsystemHealth {
    SubsystemStatus status{SubsystemStatus::NotInitialized};
    NativeError lastError{NativeError::OK};
};

struct FeatureHealth {
    bool coreReady{false};
    SubsystemHealth jni{SubsystemStatus::NotInitialized, NativeError::OK};
    SubsystemHealth io{SubsystemStatus::NotInitialized, NativeError::OK};
    SubsystemHealth fileSystemHook{SubsystemStatus::NotInitialized, NativeError::OK};
    SubsystemHealth binderHook{SubsystemStatus::NotInitialized, NativeError::OK};
    SubsystemHealth dexFileHook{SubsystemStatus::NotInitialized, NativeError::OK};
    SubsystemHealth vmClassLoaderHook{SubsystemStatus::NotInitialized, NativeError::OK};
    SubsystemHealth antiDetection{SubsystemStatus::NotInitialized, NativeError::OK};
    NativeError lastError{NativeError::OK};
};

} // namespace blackbox

#endif // BCORE_NATIVEERROR_H
