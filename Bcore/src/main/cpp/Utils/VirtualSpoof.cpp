#include "VirtualSpoof.h"
#include <jni.h>
#include <string>
#include <cstring>
#include <dlfcn.h>
#include <android/log.h>
#include "bytehook.h"

#define TAG "VirtualSpoof"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

namespace blackbox {

static int my_system_property_get(const char *name, char *value) {
    if (name == nullptr || value == nullptr) {
        return 0;
    }

    std::string prop_name(name);

    if (prop_name == "ro.product.model") {
        strcpy(value, "Spoofed Model X");
        return strlen(value);
    } else if (prop_name == "ro.product.manufacturer") {
        strcpy(value, "Spoofed Manufacturer");
        return strlen(value);
    } else if (prop_name == "ro.build.tags") {
        strcpy(value, "release-keys");
        return strlen(value);
    } else if (prop_name == "ro.build.type") {
        strcpy(value, "user");
        return strlen(value);
    } else if (prop_name == "ro.secure") {
        strcpy(value, "1");
        return strlen(value);
    }
    BYTEHOOK_STACK_SCOPE();
    return BYTEHOOK_CALL_PREV(my_system_property_get, name, value);
}

void VirtualSpoof::init() {
    bytehook_init(BYTEHOOK_MODE_AUTOMATIC, false);
    bytehook_hook_all(nullptr, "__system_property_get", (void*)my_system_property_get, nullptr, nullptr);
    LOGD("VirtualSpoof: bytehook installed successfully for __system_property_get");
}

} // namespace blackbox

extern "C" JNIEXPORT void JNICALL Java_com_equinox_virtual_core_VirtualSpoof_initSpoof(JNIEnv *env, jclass clazz) {
    blackbox::VirtualSpoof::init();
}
