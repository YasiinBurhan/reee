#include <cstring>
#include "VMClassLoaderHook.h"
#include <JniHook/JniHook.h>
#include <Utils/SafeJni.h>

namespace blackbox {

static bool hideXposedClass = false;

HOOK_JNI(jobject, findLoadedClass, JNIEnv *env, jobject obj, jobject class_loader, jstring name) {
    if (name == nullptr) {
        return orig_findLoadedClass(env, obj, class_loader, name);
    }
    
    ScopedUtfChars nameC(env, name);
    if (nameC.c_str() != nullptr && hideXposedClass) {
        const char *str = nameC.c_str();
        if (strstr(str, "de/robv/android/xposed/") ||
            strstr(str, "me/weishu/epic") ||
            strstr(str, "me/weishu/exposed") ||
            strstr(str, "de.robv.android") ||
            strstr(str, "me.weishu.epic") ||
            strstr(str, "me.weishu.exposed")) {
            return nullptr;
        }
    }
    return orig_findLoadedClass(env, obj, class_loader, name);
}

void VMClassLoaderHook::init(JNIEnv *env) {
    const char *className = "java/lang/VMClassLoader";
    JniHook::HookJniFun(env, className, "findLoadedClass", "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;",
                        (void *) new_findLoadedClass,
                        (void **) (&orig_findLoadedClass), true);
}

void VMClassLoaderHook::hideXposed() {
    hideXposedClass = true;
}

} // namespace blackbox
