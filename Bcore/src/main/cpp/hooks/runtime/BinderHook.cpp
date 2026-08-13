#include "hooks/runtime/BinderHook.h"
#include "io/IO.h"
#include "core/BoxCore.h"
#include "hooks/filesystem/UnixFileSystemHook.h"
#include "hooks/jni/JniHook.h"

namespace blackbox {

HOOK_JNI(jint, getCallingUid, JNIEnv *env, jobject obj) {
    int orig = orig_getCallingUid(env, obj);
    return BoxCore::getCallingUid(env, orig);
}

void BinderHook::init(JNIEnv *env) {
    const char *clazz = "android/os/Binder";
    JniHook::HookJniFun(env, clazz, "getCallingUid", "()I", (void *) new_getCallingUid,
                        (void **) (&orig_getCallingUid), true);
}

} // namespace blackbox
