cmake_minimum_required(VERSION 3.22.1)
project(blackbox)

# shadowhook library
add_subdirectory(third_party/shadowhook/shadowhook/src/main/cpp)

# imgui render library
add_subdirectory(third_party/imgui_render)

# Source grouping
set(BCORE_CORE_SOURCES
    core/BoxCore.cpp
)

set(BCORE_JNI_SOURCES
    jni/JniBridge.cpp
)

set(BCORE_HOOK_SOURCES
    hooks/BaseHook.cpp
    hooks/filesystem/FileSystemHook.cpp
    hooks/filesystem/UnixFileSystemHook.cpp
    hooks/runtime/VMClassLoaderHook.cpp
    hooks/runtime/DexFileHook.cpp
    hooks/runtime/BinderHook.cpp
    hooks/jni/JniHook.cpp
    hooks/gui/ImGuiHook.cpp
)

set(BCORE_IO_SOURCES
    io/IO.cpp
)

set(BCORE_SECURITY_SOURCES
    security/AntiDetection.cpp
    security/VirtualSpoof.cpp
)

set(BCORE_RUNTIME_SOURCES
    runtime/hidden_api.cpp
)

set(BCORE_UTILS_SOURCES
    utils/HexDump.cpp
)

set(BCORE_IL2CPP_SOURCES
    il2cpp/IL2CPPAPI.cpp
    il2cpp/IL2CPPTouchHook.cpp
)

option(BCORE_DIAGNOSTICS "Enable diagnostics support" OFF)
if(BCORE_DIAGNOSTICS)
    set(BCORE_DIAGNOSTIC_SOURCES
        diagnostics/Diagnostics.cpp
    )
else()
    set(BCORE_DIAGNOSTIC_SOURCES)
endif()

# blackbox shared library
add_library(blackbox SHARED
    ${BCORE_CORE_SOURCES}
    ${BCORE_JNI_SOURCES}
    ${BCORE_HOOK_SOURCES}
    ${BCORE_IO_SOURCES}
    ${BCORE_SECURITY_SOURCES}
    ${BCORE_RUNTIME_SOURCES}
    ${BCORE_UTILS_SOURCES}
    ${BCORE_IL2CPP_SOURCES}
    ${BCORE_DIAGNOSTIC_SOURCES}
)

if(BCORE_DIAGNOSTICS)
    target_compile_definitions(blackbox PRIVATE BCORE_DIAGNOSTICS=1)
else()
    target_compile_definitions(blackbox PRIVATE BCORE_DIAGNOSTICS=0)
endif()

target_include_directories(blackbox PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/core
    ${CMAKE_CURRENT_SOURCE_DIR}/jni
    ${CMAKE_CURRENT_SOURCE_DIR}/hooks
    ${CMAKE_CURRENT_SOURCE_DIR}/io
    ${CMAKE_CURRENT_SOURCE_DIR}/security
    ${CMAKE_CURRENT_SOURCE_DIR}/runtime
    ${CMAKE_CURRENT_SOURCE_DIR}/utils
    ${CMAKE_CURRENT_SOURCE_DIR}/diagnostics
    ${CMAKE_CURRENT_SOURCE_DIR}/il2cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/imgui
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/imgui_render
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/shadowhook/shadowhook/src/main/cpp/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/shadowhook/shadowhook/src/main/cpp
)

target_compile_options(blackbox PRIVATE
    -Wno-error=format-security
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
    -std=c++17
    -fms-extensions
)

target_link_options(blackbox PRIVATE
    -Wl,--gc-sections,--strip-all,-z,max-page-size=16384
)

target_link_libraries(blackbox
    PRIVATE
    shadowhook
    imgui_render
    log
    android
    EGL
    GLESv3
    z
)


