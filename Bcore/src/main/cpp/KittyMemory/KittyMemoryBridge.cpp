#include <jni.h>
#include <android/log.h>
#include "MemoryPatch.h"

#define TAG "KittyMemoryBridge"
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_equinox_virtual_helper_ModInjectionManager_nativeApplyMemoryPatch(
        JNIEnv *env,
        jobject thiz,
        jstring jlibName,
        jlong jOffset,
        jstring jHexBytes) {
    const char *libName = env->GetStringUTFChars(jlibName, nullptr);
    const char *hexBytes = env->GetStringUTFChars(jHexBytes, nullptr);

    ALOGD("%s: Applying patch to lib: %s at offset: 0x%lX with hex: %s", TAG, libName, (unsigned long)jOffset, hexBytes);

    auto patch = KittyMemory::MemoryPatch::createWithHex(libName, (uintptr_t)jOffset, std::string(hexBytes));

    jlong patchPtr = 0;
    if (patch.isValid()) {
        if (patch.modify()) {
            ALOGD("%s: Patch applied successfully!", TAG);
            auto *heapPatch = new KittyMemory::MemoryPatch(patch);
            patchPtr = reinterpret_cast<jlong>(heapPatch);
        } else {
            ALOGE("%s: Failed to modify memory for patch!", TAG);
        }
    } else {
        ALOGE("%s: Invalid memory patch or library map not found for %s", TAG, libName);
    }

    env->ReleaseStringUTFChars(jlibName, libName);
    env->ReleaseStringUTFChars(jHexBytes, hexBytes);
    return patchPtr;
}

JNIEXPORT jboolean JNICALL
Java_com_equinox_virtual_helper_ModInjectionManager_nativeRestoreMemoryPatch(
        JNIEnv *env,
        jobject thiz,
        jlong jPatchPtr) {
    if (jPatchPtr == 0) return JNI_FALSE;
    auto *patch = reinterpret_cast<KittyMemory::MemoryPatch *>(jPatchPtr);
    bool success = patch->restore();
    if (success) {
        ALOGD("%s: Patch restored successfully!", TAG);
    } else {
        ALOGE("%s: Failed to restore patch!", TAG);
    }
    return success ? JNI_TRUE : JNI_FALSE;
}

}
