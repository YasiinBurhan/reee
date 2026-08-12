#ifndef VIRTUALM_BASEHOOK_H
#define VIRTUALM_BASEHOOK_H

#include <jni.h>
#include <string>
#include <vector>

namespace blackbox {

struct HookInfo {
    std::string className;
    std::string methodName;
    std::string signature;
    void* newFunc;
    void** origFunc;
    bool isStatic;
};

class BaseHook {
public:
    virtual ~BaseHook() = default;
    virtual void onInit(JNIEnv *env) = 0;
    
    static void init(JNIEnv *env);
};

} // namespace blackbox

#endif // VIRTUALM_BASEHOOK_H
