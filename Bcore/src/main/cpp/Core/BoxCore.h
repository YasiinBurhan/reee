#ifndef BLACKBOX_BOXCORE_H
#define BLACKBOX_BOXCORE_H

#include <jni.h>

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
    static int api_level;

    static JNIEnv *getEnv();
    static JNIEnv *ensureEnvCreated();
    static void nativeHook(JNIEnv *env);

    static JavaVM *getJavaVM();
    static int getApiLevel();
    static int getCallingUid(JNIEnv *env, int orig);
    static jstring redirectPathString(JNIEnv *env, jstring path);
    static jobject redirectPathFile(JNIEnv *env, jobject path);
    static jlongArray loadEmptyDex(JNIEnv *env);
};

} // namespace blackbox

#endif // BLACKBOX_BOXCORE_H
