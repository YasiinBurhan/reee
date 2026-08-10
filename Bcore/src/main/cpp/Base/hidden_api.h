#ifndef BLACKBOX2_HIDDEN_API_H
#define BLACKBOX2_HIDDEN_API_H

#include <jni.h>

namespace blackbox {

class HiddenApi {
public:
    static bool disableHiddenApi(JNIEnv* env);
    static bool disableResourceLoading();
};

} // namespace blackbox

#endif // BLACKBOX2_HIDDEN_API_H
