#ifndef BLACKBOX_BOXCORE_H
#define BLACKBOX_BOXCORE_H

#include <jni.h>
#include <mutex>
#include <atomic>
#include <Base/NativeError.h>

#define VMCORE_CLASS "com/equinox/virtual/core/NativeCore"

namespace blackbox {

class BoxCore {
public:
    static JavaVM *vm;
    static jclass NativeCoreClass;
    static jmethodID getCallingUidId;
    static jmethodID redirectPathStringId;
    static jmethodID redirectPathFileId;
    static jmethodID loadEmptyDexId;
    static jmethodID loadEmptyDexLId;
    static std::atomic<int> api_level;
    static std::atomic<InitState> initState;
    static FeatureHealth health;
    static std::mutex init_mutex;
    static std::condition_variable init_cv;

    static JNIEnv *getEnv();
    static JNIEnv *ensureEnvCreated();
    static void nativeHook(JNIEnv *env);
    static void shutdown(JNIEnv *env);

    static JavaVM *getJavaVM();
    static int getApiLevel();
    static InitState getInitState();
    static FeatureHealth getHealth();
    static int getCallingUid(JNIEnv *env, int orig);
    static jstring redirectPathString(JNIEnv *env, jstring path);
    static jobject redirectPathFile(JNIEnv *env, jobject path);
    static jlongArray loadEmptyDex(JNIEnv *env);
};

} // namespace blackbox

#endif // BLACKBOX_BOXCORE_H
