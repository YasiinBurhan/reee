#ifndef IL2CPP_API_H
#define IL2CPP_API_H

#include <stdint.h>
#include <stddef.h>

// Forward declarations of opaque IL2CPP types
struct Il2CppDomain;
struct Il2CppAssembly;
struct Il2CppImage;
struct Il2CppClass;
struct MethodInfo;
struct FieldInfo;
struct PropertyInfo;
struct Il2CppType;
struct Il2CppObject;
struct Il2CppString;
struct Il2CppArray;
struct Il2CppReflectionType;
struct Il2CppReflectionMethod;
struct Il2CppThread;
struct Il2CppException;

// Function pointer typedefs for Unity IL2CPP Exported C APIs
typedef Il2CppDomain* (*il2cpp_domain_get_t)();
typedef const Il2CppAssembly** (*il2cpp_domain_get_assemblies_t)(const Il2CppDomain* domain, size_t* size);
typedef const Il2CppImage* (*il2cpp_assembly_get_image_t)(const Il2CppAssembly* assembly);

typedef const char* (*il2cpp_image_get_name_t)(const Il2CppImage* image);
typedef size_t (*il2cpp_image_get_class_count_t)(const Il2CppImage* image);
typedef const Il2CppClass* (*il2cpp_image_get_class_t)(const Il2CppImage* image, size_t index);
typedef Il2CppClass* (*il2cpp_class_from_name_t)(const Il2CppImage* image, const char* namespaze, const char* name);
typedef Il2CppClass* (*il2cpp_class_from_type_t)(const Il2CppType* type);
typedef const char* (*il2cpp_class_get_name_t)(Il2CppClass* klass);
typedef const char* (*il2cpp_class_get_namespace_t)(Il2CppClass* klass);
typedef Il2CppClass* (*il2cpp_class_get_parent_t)(Il2CppClass* klass);
typedef bool (*il2cpp_class_is_valuetype_t)(const Il2CppClass* klass);
typedef bool (*il2cpp_class_is_enum_t)(const Il2CppClass* klass);

typedef const MethodInfo* (*il2cpp_class_get_methods_t)(Il2CppClass* klass, void** iter);
typedef const MethodInfo* (*il2cpp_class_get_method_from_name_t)(Il2CppClass* klass, const char* name, int argsCount);
typedef const char* (*il2cpp_method_get_name_t)(const MethodInfo* method);
typedef Il2CppClass* (*il2cpp_method_get_class_t)(const MethodInfo* method);
typedef uint32_t (*il2cpp_method_get_flags_t)(const MethodInfo* method, uint32_t* iflags);
typedef uint32_t (*il2cpp_method_get_param_count_t)(const MethodInfo* method);
typedef const Il2CppType* (*il2cpp_method_get_param_t)(const MethodInfo* method, uint32_t index);
typedef const char* (*il2cpp_method_get_param_name_t)(const MethodInfo* method, uint32_t index);
typedef const Il2CppType* (*il2cpp_method_get_return_type_t)(const MethodInfo* method);

typedef FieldInfo* (*il2cpp_class_get_fields_t)(Il2CppClass* klass, void** iter);
typedef FieldInfo* (*il2cpp_class_get_field_from_name_t)(Il2CppClass* klass, const char* name);
typedef const char* (*il2cpp_field_get_name_t)(FieldInfo* field);
typedef size_t (*il2cpp_field_get_offset_t)(FieldInfo* field);
typedef const Il2CppType* (*il2cpp_field_get_type_t)(FieldInfo* field);
typedef void (*il2cpp_field_static_get_value_t)(FieldInfo* field, void* value);
typedef void (*il2cpp_field_static_set_value_t)(FieldInfo* field, void* value);
typedef void (*il2cpp_field_get_value_t)(Il2CppObject* obj, FieldInfo* field, void* value);
typedef void (*il2cpp_field_set_value_t)(Il2CppObject* obj, FieldInfo* field, void* value);

typedef const PropertyInfo* (*il2cpp_class_get_properties_t)(Il2CppClass* klass, void** iter);
typedef const PropertyInfo* (*il2cpp_class_get_property_from_name_t)(Il2CppClass* klass, const char* name);
typedef const MethodInfo* (*il2cpp_property_get_get_method_t)(const PropertyInfo* prop);
typedef const MethodInfo* (*il2cpp_property_get_set_method_t)(const PropertyInfo* prop);

typedef Il2CppObject* (*il2cpp_object_new_t)(const Il2CppClass* klass);
typedef Il2CppString* (*il2cpp_string_new_t)(const char* str);
typedef Il2CppString* (*il2cpp_string_new_utf16_t)(const uint16_t* str, int32_t len);
typedef Il2CppArray* (*il2cpp_array_new_t)(Il2CppClass* element_class, uintptr_t length);
typedef Il2CppObject* (*il2cpp_runtime_invoke_t)(const MethodInfo* method, void* obj, void** params, Il2CppException** exc);

typedef Il2CppThread* (*il2cpp_thread_attach_t)(Il2CppDomain* domain);
typedef void (*il2cpp_thread_detach_t)(Il2CppThread* thread);
typedef void* (*il2cpp_resolve_icall_t)(const char* name);

typedef uint32_t (*il2cpp_gchandle_new_t)(Il2CppObject* obj, bool pinned);
typedef void (*il2cpp_gchandle_free_t)(uint32_t gchandle);
typedef Il2CppObject* (*il2cpp_gchandle_get_target_t)(uint32_t gchandle);

// Global Function Pointers
extern il2cpp_domain_get_t il2cpp_domain_get;
extern il2cpp_domain_get_assemblies_t il2cpp_domain_get_assemblies;
extern il2cpp_assembly_get_image_t il2cpp_assembly_get_image;

extern il2cpp_image_get_name_t il2cpp_image_get_name;
extern il2cpp_image_get_class_count_t il2cpp_image_get_class_count;
extern il2cpp_image_get_class_t il2cpp_image_get_class;
extern il2cpp_class_from_name_t il2cpp_class_from_name;
extern il2cpp_class_from_type_t il2cpp_class_from_type;
extern il2cpp_class_get_name_t il2cpp_class_get_name;
extern il2cpp_class_get_namespace_t il2cpp_class_get_namespace;
extern il2cpp_class_get_parent_t il2cpp_class_get_parent;
extern il2cpp_class_is_valuetype_t il2cpp_class_is_valuetype;
extern il2cpp_class_is_enum_t il2cpp_class_is_enum;

extern il2cpp_class_get_methods_t il2cpp_class_get_methods;
extern il2cpp_class_get_method_from_name_t il2cpp_class_get_method_from_name;
extern il2cpp_method_get_name_t il2cpp_method_get_name;
extern il2cpp_method_get_class_t il2cpp_method_get_class;
extern il2cpp_method_get_flags_t il2cpp_method_get_flags;
extern il2cpp_method_get_param_count_t il2cpp_method_get_param_count;
extern il2cpp_method_get_param_t il2cpp_method_get_param;
extern il2cpp_method_get_param_name_t il2cpp_method_get_param_name;
extern il2cpp_method_get_return_type_t il2cpp_method_get_return_type;

extern il2cpp_class_get_fields_t il2cpp_class_get_fields;
extern il2cpp_class_get_field_from_name_t il2cpp_class_get_field_from_name;
extern il2cpp_field_get_name_t il2cpp_field_get_name;
extern il2cpp_field_get_offset_t il2cpp_field_get_offset;
extern il2cpp_field_get_type_t il2cpp_field_get_type;
extern il2cpp_field_static_get_value_t il2cpp_field_static_get_value;
extern il2cpp_field_static_set_value_t il2cpp_field_static_set_value;
extern il2cpp_field_get_value_t il2cpp_field_get_value;
extern il2cpp_field_set_value_t il2cpp_field_set_value;

extern il2cpp_class_get_properties_t il2cpp_class_get_properties;
extern il2cpp_class_get_property_from_name_t il2cpp_class_get_property_from_name;
extern il2cpp_property_get_get_method_t il2cpp_property_get_get_method;
extern il2cpp_property_get_set_method_t il2cpp_property_get_set_method;

extern il2cpp_object_new_t il2cpp_object_new;
extern il2cpp_string_new_t il2cpp_string_new;
extern il2cpp_string_new_utf16_t il2cpp_string_new_utf16;
extern il2cpp_array_new_t il2cpp_array_new;
extern il2cpp_runtime_invoke_t il2cpp_runtime_invoke;

extern il2cpp_thread_attach_t il2cpp_thread_attach;
extern il2cpp_thread_detach_t il2cpp_thread_detach;
extern il2cpp_resolve_icall_t il2cpp_resolve_icall;

extern il2cpp_gchandle_new_t il2cpp_gchandle_new;
extern il2cpp_gchandle_free_t il2cpp_gchandle_free;
extern il2cpp_gchandle_get_target_t il2cpp_gchandle_get_target;

class IL2CPPAPI {
public:
    // Initialize IL2CPP symbol bindings via shadowhook_dlopen and shadowhook_dlsym
    static bool Init(const char* libName = "libil2cpp.so");

    // Helper utilities
    static const Il2CppImage* GetImage(const char* assemblyName);
    static Il2CppClass* GetClass(const char* assemblyName, const char* namespaze, const char* klassName);
    static const MethodInfo* GetMethod(const char* assemblyName, const char* namespaze, const char* klassName, const char* methodName, int argsCount = -1);
    static void* GetMethodPointer(const char* assemblyName, const char* namespaze, const char* klassName, const char* methodName, int argsCount = -1);
    static size_t GetFieldOffset(const char* assemblyName, const char* namespaze, const char* klassName, const char* fieldName);
    static Il2CppString* CreateString(const char* str);
    static Il2CppThread* AttachCurrentThread();
};

#endif // IL2CPP_API_H
