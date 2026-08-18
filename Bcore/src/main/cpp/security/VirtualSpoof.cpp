#include "security/VirtualSpoof.h"
#include <jni.h>
#include <string>
#include <cstring>
#include <dlfcn.h>
#include <android/log.h>
#include "shadowhook.h"

#define TAG "VirtualSpoof"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

namespace blackbox {

void VirtualSpoof::init() {
    LOGD("VirtualSpoof: Subsystem in passive mode");
}

} // namespace blackbox

extern "C" JNIEXPORT void JNICALL Java_com_equinox_virtual_core_VirtualSpoof_initSpoof(JNIEnv * /*env*/, jclass /*clazz*/) {
    blackbox::VirtualSpoof::init();
}
