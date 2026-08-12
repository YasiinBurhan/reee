cmake_minimum_required(VERSION 3.22.1)
project(blackbox)

# shadowhook library
add_subdirectory(shadowhook/shadowhook/src/main/cpp)

add_subdirectory(imgui_render)


# blackbox shared library
add_library(blackbox SHARED
    Core/BoxCore.cpp
    Jni/JniBridge.cpp
    KittyMemory/KittyMemory.cpp
    KittyMemory/KittyScanner.cpp
    KittyMemory/KittyUtils.cpp
    KittyMemory/KittyIOFile.cpp
    KittyMemory/KittyAsm.cpp
    KittyMemory/KittyPtrValidator.cpp
    KittyMemory/MemoryBackup.cpp
    KittyMemory/MemoryPatch.cpp
    KittyMemory/KittyMemoryBridge.cpp
    Base/hidden_api.cpp
    Base/IO.cpp
    Hook/DexFileHook.cpp
    Hook/FileSystemHook.cpp
    Utils/VirtualSpoof.cpp
    Utils/HexDump.cpp
    Utils/AntiDetection.cpp
    Utils/TestHarness.cpp
    Hook/VMClassLoaderHook.cpp
    Hook/UnixFileSystemHook.cpp
    Hook/BinderHook.cpp
    Hook/BaseHook.cpp
    Hook/ImGuiHook.cpp
    JniHook/JniHook.cpp
    il2cpp/IL2CPPAPI.cpp
    il2cpp/IL2CPPTouchHook.cpp
)

target_include_directories(blackbox PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_SOURCE_DIR}/Base ${CMAKE_CURRENT_SOURCE_DIR}/Jni ${CMAKE_CURRENT_SOURCE_DIR}/imgui ${CMAKE_CURRENT_SOURCE_DIR}/imgui_render ${CMAKE_CURRENT_SOURCE_DIR}/il2cpp ${CMAKE_CURRENT_SOURCE_DIR}/shadowhook/shadowhook/src/main/cpp/include ${CMAKE_CURRENT_SOURCE_DIR}/shadowhook/shadowhook/src/main/cpp)

target_compile_options(blackbox PRIVATE
    -Wall
    -Wextra
    -Wno-unused-parameter
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -std=c++17
    -fms-extensions
    -fPIC
)

if(NOT DEFINED BCORE_DIAGNOSTICS)
    if(CMAKE_BUILD_TYPE STREQUAL "Debug")
        set(BCORE_DIAGNOSTICS 1)
    else()
        set(BCORE_DIAGNOSTICS 0)
    endif()
endif()

target_compile_definitions(blackbox PRIVATE BCORE_DIAGNOSTICS=${BCORE_DIAGNOSTICS})

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

