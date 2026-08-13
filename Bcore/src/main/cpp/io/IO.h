#ifndef VIRTUALM_IO_H
#define VIRTUALM_IO_H

#include <jni.h>
#include <list>
#include <iostream>
#include <string>

namespace blackbox {

class IO {
public:
    static void init(JNIEnv *env);

    struct RelocateInfo {
        std::string targetPath;
        std::string relocatePath;
    };

    static void addRule(const char *targetPath, const char *relocatePath);
    static jstring redirectPath(JNIEnv *env, jstring path);
    static jobject redirectPath(JNIEnv *env, jobject path);
    static const char *redirectPath(const char *__path);

    // For diagnostics save / restore state
    static std::list<RelocateInfo> getRules();
    static void setRules(const std::list<RelocateInfo> &rules);
};

} // namespace blackbox

#endif // VIRTUALM_IO_H
