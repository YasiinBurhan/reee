#ifndef BLACKBOX_BINDERHOOK_H
#define BLACKBOX_BINDERHOOK_H

#include "hooks/BaseHook.h"

namespace blackbox {

class BinderHook : public BaseHook {
public:
    static void init(JNIEnv *env);
};

} // namespace blackbox

#endif // BLACKBOX_BINDERHOOK_H
