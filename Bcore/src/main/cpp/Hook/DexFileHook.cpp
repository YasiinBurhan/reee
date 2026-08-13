#include "DexFileHook.h"
#include <Base/IO.h>
#include <Core/BoxCore.h>
#include "UnixFileSystemHook.h"
#include <JniHook/JniHook.h>
#include <Base/JniUtils.h>
#include <sys/stat.h>
#include "Log.h"

namespace blackbox {

HOOK_JNI(jobject, openDexFileNative, JNIEnv *env, jobject obj, jstring sourceName, jstring outputName, jint flags, jobject loader, jobject elements) {
    if (!sourceName) {
        return orig_openDexFileNative(env, obj, sourceName, outputName, flags, loader, elements);
    }
    ScopedUtfChars sourceNameC(env, sourceName);
    ALOGD("openDexFileNative: %s", sourceNameC.c_str());
    if (strstr(sourceNameC.c_str(), "/blackbox/") != nullptr) {
        DexFileHook::setFileReadonly(sourceNameC.c_str());
    }
    return orig_openDexFileNative(env, obj, sourceName, outputName, flags, loader, elements);
}

void DexFileHook::init(JNIEnv *env) {
    if (BoxCore::getApiLevel() >= __ANDROID_API_U__) {
        const char *clazz = "dalvik/system/DexFile";
        JniHook::HookJniFun(env, clazz, "openDexFileNative", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/ClassLoader;[Ldalvik/system/DexPathList$Element;)Ljava/lang/Object;", (void *) new_openDexFileNative,
                            (void **) (&orig_openDexFileNative), true);
    }
}

void DexFileHook::setFileReadonly(const char* filePath) {
    struct stat fileStat;
    if (stat(filePath, &fileStat) != 0) {
        ALOGD("DexFileHook::setFileReadonly: %s 不存在", filePath);
        return;
    }
    if (chmod(filePath, S_IRUSR) != 0) {
        ALOGD("DexFileHook::setFileReadonly: 设置文件 %s 为只读时出错", filePath);
    } else {
        ALOGD("DexFileHook::setFileReadonly: 设置文件 %s 为只读成功", filePath);
    }
}

} // namespace blackbox
