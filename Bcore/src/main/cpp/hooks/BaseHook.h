#ifndef VIRTUALM_BASEHOOK_H
#define VIRTUALM_BASEHOOK_H

#include <jni.h>

namespace blackbox {

class BaseHook {
public:
    static void init(JNIEnv *env);
};

} // namespace blackbox

#endif // VIRTUALM_BASEHOOK_H
