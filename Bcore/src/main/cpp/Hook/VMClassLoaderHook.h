#ifndef BLACKBOX_VMCLASSLOADERHOOK_H
#define BLACKBOX_VMCLASSLOADERHOOK_H

#include "BaseHook.h"
#include <jni.h>

namespace blackbox {

class VMClassLoaderHook : public BaseHook {
public:
    static void hideXposed();
    static void init(JNIEnv *env);
};

} // namespace blackbox

#endif // BLACKBOX_VMCLASSLOADERHOOK_H
