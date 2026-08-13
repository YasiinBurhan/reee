#include <jni.h>
#include <string>
#include "utils/JniUtils.h"
#include "hooks/jni/JniHook.h"
#include "utils/Log.h"
#include "hooks/jni/ArtMethod.h"

namespace blackbox {

static struct {
    int api_level;
    unsigned int art_field_size;
    int art_field_flags_offset;

    unsigned int art_method_size;
    int art_method_flags_offset;
    int art_method_native_offset;

    int class_flags_offset;

    jclass method_utils_class;
    jmethodID get_method_desc_id;
    jmethodID get_method_declaring_class_id;
    jmethodID get_method_name_id;

} HookEnv;

static std::string GetMethodDesc(JNIEnv *env, jobject javaMethod) {
    auto desc = reinterpret_cast<jstring>(env->CallStaticObjectMethod(HookEnv.method_utils_class,
                                                                      HookEnv.get_method_desc_id,
                                                                      javaMethod));
    if (!desc) return "";
    ScopedUtfChars utf(env, desc);
    std::string result = utf.c_str();
    env->DeleteLocalRef(desc);
    return result;
}

static std::string GetMethodDeclaringClass(JNIEnv *env, jobject javaMethod) {
    auto desc = reinterpret_cast<jstring>(env->CallStaticObjectMethod(HookEnv.method_utils_class,
                                                                      HookEnv.get_method_declaring_class_id,
                                                                      javaMethod));
    if (!desc) return "";
    ScopedUtfChars utf(env, desc);
    std::string result = utf.c_str();
    env->DeleteLocalRef(desc);
    return result;
}

static std::string GetMethodName(JNIEnv *env, jobject javaMethod) {
    auto desc = reinterpret_cast<jstring>(env->CallStaticObjectMethod(HookEnv.method_utils_class,
                                                                      HookEnv.get_method_name_id,
                                                                      javaMethod));
    if (!desc) return "";
    ScopedUtfChars utf(env, desc);
    std::string result = utf.c_str();
    env->DeleteLocalRef(desc);
    return result;
}

inline static uint32_t GetAccessFlags(const char *art_method) {
    return *reinterpret_cast<const uint32_t *>(art_method + HookEnv.art_method_flags_offset);
}

inline static bool SetAccessFlags(char *art_method, uint32_t flags) {
    *reinterpret_cast<uint32_t *>(art_method + HookEnv.art_method_flags_offset) = flags;
    return true;
}

inline static bool AddAccessFlag(char *art_method, uint32_t flag) {
    uint32_t old_flag = GetAccessFlags(art_method);
    uint32_t new_flag = old_flag | flag;
    return new_flag != old_flag && SetAccessFlags(art_method, new_flag);
}

inline static bool ClearAccessFlag(char *art_method, uint32_t flag) {
    uint32_t old_flag = GetAccessFlags(art_method);
    uint32_t new_flag = old_flag & ~flag;
    return new_flag != old_flag && SetAccessFlags(art_method, new_flag);
}

inline static bool HasAccessFlag(char *art_method, uint32_t flag) {
    uint32_t flags = GetAccessFlags(art_method);
    ALOGD("AccessFlag:flags = 0x%x,flag = 0x%x", flags, flag);
    return (flags & flag) == flag;
}

[[maybe_unused]] inline static bool IsNativeMethod(char *art_method) {
    try {
        return HasAccessFlag(art_method, kAccNative);
    } catch (...) {
        ALOGD("NativeCore: Error checking native method flag, assuming not native");
        return false;
    }
}

inline static bool ClearFastNativeFlag(char *art_method) {
    return HookEnv.api_level < __ANDROID_API_P__ && ClearAccessFlag(art_method, kAccFastNative);
}

static void *GetArtMethod(JNIEnv *env, jclass clazz, jmethodID methodId) {
    if (HookEnv.api_level >= __ANDROID_API_Q__) {
        ScopedLocalRef<jclass> executable(env, env->FindClass("java/lang/reflect/Executable"));
        if (executable.empty()) return nullptr;
        jfieldID artId = env->GetFieldID(executable, "artMethod", "J");
        jobject method = env->ToReflectedMethod(clazz, methodId, true);
        if (!method) return nullptr;
        void *result = reinterpret_cast<void *>(env->GetLongField(method, artId));
        env->DeleteLocalRef(method);
        return result;
    } else {
        return methodId;
    }
}

static void *GetFieldMethod(JNIEnv *env, jobject field) {
    if (HookEnv.api_level >= __ANDROID_API_Q__) {
        ScopedLocalRef<jclass> fieldClass(env, env->FindClass("java/lang/reflect/Field"));
        if (fieldClass.empty()) return nullptr;
        jmethodID getArtField = env->GetMethodID(fieldClass, "getArtField", "()J");
        return reinterpret_cast<void *>(env->CallLongMethod(field, getArtField));
    } else {
        return env->FromReflectedField(field);
    }
}

static bool CheckFlags(void *artMethod) {
    char *method = static_cast<char *>(artMethod);
    try {
        if (!HasAccessFlag(method, kAccNative)) {
            ALOGD("Method is not native, skipping hook");
            return false;
        }
        ClearFastNativeFlag(method);
        return true;
    } catch (...) {
        ALOGD("Error checking method flags, assuming not native");
        return false;
    }
}

void JniHook::HookJniFun(JNIEnv *env, jobject java_method, void *new_fun,
                         void **orig_fun, bool is_static) {
    std::string class_name = GetMethodDeclaringClass(env, java_method);
    std::string method_name = GetMethodName(env, java_method);
    std::string sign = GetMethodDesc(env, java_method);
    HookJniFun(env, class_name.c_str(), method_name.c_str(), sign.c_str(), new_fun, orig_fun, is_static);
}

void JniHook::HookJniFun(JNIEnv *env, const char *class_name, const char *method_name, const char *sign,
                         void *new_fun, void **orig_fun, bool is_static) {
    if (HookEnv.art_method_native_offset == 0) {
        return;
    }
    ScopedLocalRef<jclass> clazz(env, env->FindClass(class_name));
    if (clazz.empty()) {
        ALOGD("findClass fail: %s %s", class_name, method_name);
        env->ExceptionClear();
        return;
    }
    jmethodID method = nullptr;
    if (is_static) {
        method = env->GetStaticMethodID(clazz, method_name, sign);
    } else {
        method = env->GetMethodID(clazz, method_name, sign);
    }
    if (!method) {
        env->ExceptionClear();
        ALOGD("get method id fail: %s %s", class_name, method_name);
        return;
    }
    JNINativeMethod gMethods[] = {
            {method_name, sign, (void *) new_fun},
    };

    auto artMethod = reinterpret_cast<uintptr_t *>(GetArtMethod(env, clazz, method));
    if (!artMethod || !CheckFlags(artMethod)) {
        ALOGD("Skipping hook for non-native method: %s.%s", class_name, method_name);
        return;
    }
    *orig_fun = reinterpret_cast<void *>(artMethod[HookEnv.art_method_native_offset]);
    if (env->RegisterNatives(clazz, gMethods, 1) < 0) {
        ALOGE("jni hook error. class：%s, method：%s", class_name, method_name);
        return;
    }
    
    if (HookEnv.api_level == __ANDROID_API_O__ || HookEnv.api_level == __ANDROID_API_O_MR1__) {
        AddAccessFlag((char *) artMethod, kAccFastNative);
    }
    ALOGD("register class：%s, method：%s success!", class_name, method_name);
}

__attribute__((section (".mytext"))) JNICALL void native_offset(JNIEnv * /*env*/, jclass /*obj*/) {}

__attribute__((section (".mytext"))) JNICALL void native_offset2(JNIEnv * /*env*/, jclass /*obj*/) {}

__attribute__((section (".mytext"))) JNICALL void set_method_accessible(JNIEnv *env, jclass /*obj*/, jclass clazz, jobject method) {
    jmethodID methodId = env->FromReflectedMethod(method);
    char *art_method = static_cast<char *>(GetArtMethod(env, clazz, methodId));
    AddAccessFlag(art_method, kAccPublic);
    if (HookEnv.api_level >= __ANDROID_API_Q__) {
        AddAccessFlag(art_method, kAccPublicApi);
    }
}

__attribute__((section (".mytext"))) JNICALL void set_field_accessible(JNIEnv *env, jclass /*obj*/, jclass /*clazz*/, jobject field) {
    char *artField = static_cast<char *>(GetFieldMethod(env, field));
    AddAccessFlag(artField, kAccPublic);
    if (HookEnv.api_level >= __ANDROID_API_Q__) {
        AddAccessFlag(artField, kAccPublicApi);
    }
    ClearAccessFlag(artField, kAccFinal);
}

static void registerNative(JNIEnv *env) {
    ScopedLocalRef<jclass> clazz(env, env->FindClass("top/niunaijun/jnihook/jni/JniHook"));
    if (clazz.empty()) return;
    JNINativeMethod gMethods[] = {
            {"nativeOffset",  "()V",                                            (void *) native_offset},
            {"nativeOffset2", "()V",                                            (void *) native_offset2},
            {"setAccessible", "(Ljava/lang/Class;Ljava/lang/reflect/Method;)V", (void *) set_method_accessible},
            {"setAccessible", "(Ljava/lang/Class;Ljava/lang/reflect/Field;)V",  (void *) set_field_accessible},
    };
    if (env->RegisterNatives(clazz, gMethods, sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
        ALOGE("jni register error.");
    }
}

void JniHook::InitJniHook(JNIEnv *env, int api_level) {
    registerNative(env);
    HookEnv.api_level = api_level;

    ScopedLocalRef<jclass> clazz(env, env->FindClass("top/niunaijun/jnihook/jni/JniHook"));
    if (clazz.empty()) return;
    jmethodID nativeOffsetId = env->GetStaticMethodID(clazz, "nativeOffset", "()V");
    jmethodID nativeOffset2Id = env->GetStaticMethodID(clazz, "nativeOffset2", "()V");

    jfieldID nativeOffsetFieldId = env->GetStaticFieldID(clazz, "NATIVE_OFFSET", "I");
    jfieldID nativeOffsetField2Id = env->GetStaticFieldID(clazz, "NATIVE_OFFSET_2", "I");

    jobject reflected1 = env->ToReflectedField(clazz, nativeOffsetFieldId, true);
    jobject reflected2 = env->ToReflectedField(clazz, nativeOffsetField2Id, true);
    void *nativeOffsetField = GetFieldMethod(env, reflected1);
    void *nativeOffsetField2 = GetFieldMethod(env, reflected2);
    if (reflected1) env->DeleteLocalRef(reflected1);
    if (reflected2) env->DeleteLocalRef(reflected2);

    HookEnv.art_field_size = (size_t) nativeOffsetField2 - (size_t) nativeOffsetField;

    void *nativeOffset = GetArtMethod(env, clazz, nativeOffsetId);
    void *nativeOffset2 = GetArtMethod(env, clazz, nativeOffset2Id);
    HookEnv.art_method_size = (size_t) nativeOffset2 - (size_t) nativeOffset;

    uint32_t i = 0;
    auto artMethod = reinterpret_cast<uintptr_t *>(nativeOffset);
    for (i = 0; i < HookEnv.art_method_size; ++i) {
        if (reinterpret_cast<void *>(artMethod[i]) == native_offset) {
            HookEnv.art_method_native_offset = i;
            break;
        }
    }
    if (i == HookEnv.art_method_size) {
        ALOGE("init jni hook error. art_method_native_offset not found!");
        return;
    }

    uint32_t flags = 0x0;
    flags = flags | kAccPublic;
    flags = flags | kAccStatic;
    flags = flags | kAccNative;
    flags = flags | kAccFinal;
    if (api_level >= __ANDROID_API_Q__) {
        flags = flags | kAccPublicApi;
    }
    if (api_level >= __ANDROID_API_S__) {
        flags = flags | kAccNterpInvokeFastPathFlag;
    }

    char *start = reinterpret_cast<char *>(artMethod);
    for (i = 1; i < HookEnv.art_method_size; ++i) {
        auto value = *(uint32_t *) (start + i * sizeof(uint32_t));

        if (value == flags) {
            HookEnv.art_method_flags_offset = i * sizeof(uint32_t);
            break;
        }
    }
    if (i == HookEnv.art_method_size) {
        ALOGE("init jni hook error. art_method_flags_offset not found!");
        return;
    }

    flags = 0x0;
    flags = flags | kAccPublic;
    flags = flags | kAccStatic;
    flags = flags | kAccFinal;
    if (api_level >= __ANDROID_API_Q__) {
        flags = flags | kAccPublicApi;
    }
    char *fieldStart = reinterpret_cast<char *>(nativeOffsetField);
    for (i = 1; i < HookEnv.art_field_size; ++i) {
        auto value = *(uint32_t *) (fieldStart + i * sizeof(uint32_t));
        if (value == flags) {
            HookEnv.art_field_flags_offset = i * sizeof(uint32_t);
            break;
        }
    }
    if (i == HookEnv.art_field_size) {
        ALOGE("init jni hook error. art_field_flags_offset not found!");
        return;
    }

    ScopedLocalRef<jclass> localUtilsClass(env, env->FindClass("top/niunaijun/jnihook/MethodUtils"));
    if (localUtilsClass.empty()) {
        ALOGE("Failed to find top/niunaijun/jnihook/MethodUtils class!");
        return;
    }
    HookEnv.method_utils_class = reinterpret_cast<jclass>(env->NewGlobalRef(localUtilsClass.get()));
    HookEnv.get_method_desc_id = env->GetStaticMethodID(HookEnv.method_utils_class, "getDesc",
                                                         "(Ljava/lang/reflect/Method;)Ljava/lang/String;");
    HookEnv.get_method_declaring_class_id = env->GetStaticMethodID(HookEnv.method_utils_class,
                                                                    "getDeclaringClass",
                                                                    "(Ljava/lang/reflect/Method;)Ljava/lang/String;");
    HookEnv.get_method_name_id = env->GetStaticMethodID(HookEnv.method_utils_class, "getMethodName",
                                                         "(Ljava/lang/reflect/Method;)Ljava/lang/String;");
}

void JniHook::DeinitJniHook(JNIEnv *env) {
    if (env && HookEnv.method_utils_class) {
        env->DeleteGlobalRef(HookEnv.method_utils_class);
        HookEnv.method_utils_class = nullptr;
    }
}

} // namespace blackbox
