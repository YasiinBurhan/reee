#include "IL2CPPAPI.h"
#include <shadowhook.h>
#include <android/log.h>
#include <string.h>

#define LOG_TAG "IL2CPPAPI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

il2cpp_domain_get_t il2cpp_domain_get = nullptr;
il2cpp_domain_get_assemblies_t il2cpp_domain_get_assemblies = nullptr;
il2cpp_assembly_get_image_t il2cpp_assembly_get_image = nullptr;

il2cpp_image_get_name_t il2cpp_image_get_name = nullptr;
il2cpp_image_get_class_count_t il2cpp_image_get_class_count = nullptr;
il2cpp_image_get_class_t il2cpp_image_get_class = nullptr;
il2cpp_class_from_name_t il2cpp_class_from_name = nullptr;
il2cpp_class_from_type_t il2cpp_class_from_type = nullptr;
il2cpp_class_get_name_t il2cpp_class_get_name = nullptr;
il2cpp_class_get_namespace_t il2cpp_class_get_namespace = nullptr;
il2cpp_class_get_parent_t il2cpp_class_get_parent = nullptr;
il2cpp_class_is_valuetype_t il2cpp_class_is_valuetype = nullptr;
il2cpp_class_is_enum_t il2cpp_class_is_enum = nullptr;

il2cpp_class_get_methods_t il2cpp_class_get_methods = nullptr;
il2cpp_class_get_method_from_name_t il2cpp_class_get_method_from_name = nullptr;
il2cpp_method_get_name_t il2cpp_method_get_name = nullptr;
il2cpp_method_get_class_t il2cpp_method_get_class = nullptr;
il2cpp_method_get_flags_t il2cpp_method_get_flags = nullptr;
il2cpp_method_get_param_count_t il2cpp_method_get_param_count = nullptr;
il2cpp_method_get_param_t il2cpp_method_get_param = nullptr;
il2cpp_method_get_param_name_t il2cpp_method_get_param_name = nullptr;
il2cpp_method_get_return_type_t il2cpp_method_get_return_type = nullptr;

il2cpp_class_get_fields_t il2cpp_class_get_fields = nullptr;
il2cpp_class_get_field_from_name_t il2cpp_class_get_field_from_name = nullptr;
il2cpp_field_get_name_t il2cpp_field_get_name = nullptr;
il2cpp_field_get_offset_t il2cpp_field_get_offset = nullptr;
il2cpp_field_get_type_t il2cpp_field_get_type = nullptr;
il2cpp_field_static_get_value_t il2cpp_field_static_get_value = nullptr;
il2cpp_field_static_set_value_t il2cpp_field_static_set_value = nullptr;
il2cpp_field_get_value_t il2cpp_field_get_value = nullptr;
il2cpp_field_set_value_t il2cpp_field_set_value = nullptr;

il2cpp_class_get_properties_t il2cpp_class_get_properties = nullptr;
il2cpp_class_get_property_from_name_t il2cpp_class_get_property_from_name = nullptr;
il2cpp_property_get_get_method_t il2cpp_property_get_get_method = nullptr;
il2cpp_property_get_set_method_t il2cpp_property_get_set_method = nullptr;

il2cpp_object_new_t il2cpp_object_new = nullptr;
il2cpp_string_new_t il2cpp_string_new = nullptr;
il2cpp_string_new_utf16_t il2cpp_string_new_utf16 = nullptr;
il2cpp_array_new_t il2cpp_array_new = nullptr;
il2cpp_runtime_invoke_t il2cpp_runtime_invoke = nullptr;

il2cpp_thread_attach_t il2cpp_thread_attach = nullptr;
il2cpp_thread_detach_t il2cpp_thread_detach = nullptr;
il2cpp_resolve_icall_t il2cpp_resolve_icall = nullptr;

il2cpp_gchandle_new_t il2cpp_gchandle_new = nullptr;
il2cpp_gchandle_free_t il2cpp_gchandle_free = nullptr;
il2cpp_gchandle_get_target_t il2cpp_gchandle_get_target = nullptr;

static void* g_Il2CppHandle = nullptr;

bool IL2CPPAPI::Init(const char* libName) {
    if (g_Il2CppHandle != nullptr) {
        return true;
    }

    if (libName == nullptr) {
        libName = "libil2cpp.so";
    }

    // Use Shadowhook's dlopen to obtain handle
    g_Il2CppHandle = shadowhook_dlopen(libName);
    if (!g_Il2CppHandle) {
        LOGE("Failed to shadowhook_dlopen %s", libName);
        return false;
    }

    LOGI("shadowhook_dlopen successfully opened %s at %p", libName, g_Il2CppHandle);

    #define RESOLVE_IL2CPP_SYM(func) \
        func = (func##_t)shadowhook_dlsym(g_Il2CppHandle, #func); \
        if (!func) { \
            func = (func##_t)shadowhook_dlsym_symtab(g_Il2CppHandle, #func); \
        } \
        if (!func) { \
            LOGE("Warning: Could not resolve IL2CPP symbol: %s", #func); \
        }

    RESOLVE_IL2CPP_SYM(il2cpp_domain_get);
    RESOLVE_IL2CPP_SYM(il2cpp_domain_get_assemblies);
    RESOLVE_IL2CPP_SYM(il2cpp_assembly_get_image);

    RESOLVE_IL2CPP_SYM(il2cpp_image_get_name);
    RESOLVE_IL2CPP_SYM(il2cpp_image_get_class_count);
    RESOLVE_IL2CPP_SYM(il2cpp_image_get_class);
    RESOLVE_IL2CPP_SYM(il2cpp_class_from_name);
    RESOLVE_IL2CPP_SYM(il2cpp_class_from_type);
    RESOLVE_IL2CPP_SYM(il2cpp_class_get_name);
    RESOLVE_IL2CPP_SYM(il2cpp_class_get_namespace);
    RESOLVE_IL2CPP_SYM(il2cpp_class_get_parent);
    RESOLVE_IL2CPP_SYM(il2cpp_class_is_valuetype);
    RESOLVE_IL2CPP_SYM(il2cpp_class_is_enum);

    RESOLVE_IL2CPP_SYM(il2cpp_class_get_methods);
    RESOLVE_IL2CPP_SYM(il2cpp_class_get_method_from_name);
    RESOLVE_IL2CPP_SYM(il2cpp_method_get_name);
    RESOLVE_IL2CPP_SYM(il2cpp_method_get_class);
    RESOLVE_IL2CPP_SYM(il2cpp_method_get_flags);
    RESOLVE_IL2CPP_SYM(il2cpp_method_get_param_count);
    RESOLVE_IL2CPP_SYM(il2cpp_method_get_param);
    RESOLVE_IL2CPP_SYM(il2cpp_method_get_param_name);
    RESOLVE_IL2CPP_SYM(il2cpp_method_get_return_type);

    RESOLVE_IL2CPP_SYM(il2cpp_class_get_fields);
    RESOLVE_IL2CPP_SYM(il2cpp_class_get_field_from_name);
    RESOLVE_IL2CPP_SYM(il2cpp_field_get_name);
    RESOLVE_IL2CPP_SYM(il2cpp_field_get_offset);
    RESOLVE_IL2CPP_SYM(il2cpp_field_get_type);
    RESOLVE_IL2CPP_SYM(il2cpp_field_static_get_value);
    RESOLVE_IL2CPP_SYM(il2cpp_field_static_set_value);
    RESOLVE_IL2CPP_SYM(il2cpp_field_get_value);
    RESOLVE_IL2CPP_SYM(il2cpp_field_set_value);

    RESOLVE_IL2CPP_SYM(il2cpp_class_get_properties);
    RESOLVE_IL2CPP_SYM(il2cpp_class_get_property_from_name);
    RESOLVE_IL2CPP_SYM(il2cpp_property_get_get_method);
    RESOLVE_IL2CPP_SYM(il2cpp_property_get_set_method);

    RESOLVE_IL2CPP_SYM(il2cpp_object_new);
    RESOLVE_IL2CPP_SYM(il2cpp_string_new);
    RESOLVE_IL2CPP_SYM(il2cpp_string_new_utf16);
    RESOLVE_IL2CPP_SYM(il2cpp_array_new);
    RESOLVE_IL2CPP_SYM(il2cpp_runtime_invoke);

    RESOLVE_IL2CPP_SYM(il2cpp_thread_attach);
    RESOLVE_IL2CPP_SYM(il2cpp_thread_detach);
    RESOLVE_IL2CPP_SYM(il2cpp_resolve_icall);

    RESOLVE_IL2CPP_SYM(il2cpp_gchandle_new);
    RESOLVE_IL2CPP_SYM(il2cpp_gchandle_free);
    RESOLVE_IL2CPP_SYM(il2cpp_gchandle_get_target);

    #undef RESOLVE_IL2CPP_SYM

    LOGI("IL2CPPAPI fully bound using shadowhook dlsym.");
    return true;
}

const Il2CppImage* IL2CPPAPI::GetImage(const char* assemblyName) {
    if (!il2cpp_domain_get || !il2cpp_domain_get_assemblies || !il2cpp_assembly_get_image || !il2cpp_image_get_name) {
        return nullptr;
    }

    Il2CppDomain* domain = il2cpp_domain_get();
    if (!domain) return nullptr;

    size_t size = 0;
    const Il2CppAssembly** assemblies = il2cpp_domain_get_assemblies(domain, &size);
    if (!assemblies) return nullptr;

    for (size_t i = 0; i < size; ++i) {
        const Il2CppImage* img = il2cpp_assembly_get_image(assemblies[i]);
        if (img) {
            const char* name = il2cpp_image_get_name(img);
            if (name && (strcmp(name, assemblyName) == 0 || strstr(name, assemblyName) != nullptr)) {
                return img;
            }
        }
    }
    return nullptr;
}

Il2CppClass* IL2CPPAPI::GetClass(const char* assemblyName, const char* namespaze, const char* klassName) {
    const Il2CppImage* img = GetImage(assemblyName);
    if (!img || !il2cpp_class_from_name) {
        return nullptr;
    }
    return il2cpp_class_from_name(img, namespaze, klassName);
}

const MethodInfo* IL2CPPAPI::GetMethod(const char* assemblyName, const char* namespaze, const char* klassName, const char* methodName, int argsCount) {
    Il2CppClass* klass = GetClass(assemblyName, namespaze, klassName);
    if (!klass) return nullptr;

    if (argsCount >= 0 && il2cpp_class_get_method_from_name) {
        return il2cpp_class_get_method_from_name(klass, methodName, argsCount);
    }

    if (!il2cpp_class_get_methods || !il2cpp_method_get_name) {
        return nullptr;
    }

    void* iter = nullptr;
    const MethodInfo* method = nullptr;
    while ((method = il2cpp_class_get_methods(klass, &iter)) != nullptr) {
        const char* name = il2cpp_method_get_name(method);
        if (name && strcmp(name, methodName) == 0) {
            if (argsCount < 0 || (il2cpp_method_get_param_count && (int)il2cpp_method_get_param_count(method) == argsCount)) {
                return method;
            }
        }
    }
    return nullptr;
}

void* IL2CPPAPI::GetMethodPointer(const char* assemblyName, const char* namespaze, const char* klassName, const char* methodName, int argsCount) {
    const MethodInfo* method = GetMethod(assemblyName, namespaze, klassName, methodName, argsCount);
    if (!method) return nullptr;
    return *(void**)method; // MethodInfo's first field is methodPointer
}

size_t IL2CPPAPI::GetFieldOffset(const char* assemblyName, const char* namespaze, const char* klassName, const char* fieldName) {
    Il2CppClass* klass = GetClass(assemblyName, namespaze, klassName);
    if (!klass || !il2cpp_class_get_field_from_name || !il2cpp_field_get_offset) {
        return 0;
    }
    FieldInfo* field = il2cpp_class_get_field_from_name(klass, fieldName);
    if (!field) return 0;
    return il2cpp_field_get_offset(field);
}

Il2CppString* IL2CPPAPI::CreateString(const char* str) {
    if (!il2cpp_string_new || !str) return nullptr;
    return il2cpp_string_new(str);
}

Il2CppThread* IL2CPPAPI::AttachCurrentThread() {
    if (!il2cpp_domain_get || !il2cpp_thread_attach) return nullptr;
    Il2CppDomain* domain = il2cpp_domain_get();
    if (!domain) return nullptr;
    return il2cpp_thread_attach(domain);
}
