#ifndef VIRTUALM_IO_H
#define VIRTUALM_IO_H

#include <jni.h>
#include <list>
#include <string>
#include <mutex>

namespace blackbox {

class IO {
public:
    static void init(JNIEnv *env);

    struct RelocateInfo {
        std::string targetPath;
        std::string relocatePath;
    };

    static void addRule(const char *targetPath, const char *relocatePath);
    static void removeRule(const char *targetPath, const char *relocatePath);
    static jstring redirectPath(JNIEnv *env, jstring path);
    static jobject redirectPath(JNIEnv *env, jobject path);
    static std::string redirectPath(const std::string& path);
};

} // namespace blackbox

#endif // VIRTUALM_IO_H
