cmake_minimum_required(VERSION 3.22.1)
project(blackbox)

# shadowhook library
add_subdirectory(shadowhook/shadowhook/src/main/cpp)

add_subdirectory(imgui_render)

add_library(xdl STATIC
    xdl/xdl.c
    xdl/xdl_iterate.c
    xdl/xdl_linker.c
    xdl/xdl_lzma.c
    xdl/xdl_util.c
)
target_include_directories(xdl PUBLIC ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_SOURCE_DIR}/Base)
target_compile_features(xdl PRIVATE cxx_std_11)
target_compile_options(xdl PRIVATE -fno-exceptions -fno-rtti -w)


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
    Utils/elf_util.cpp
    Hook/DexFileHook.cpp
    Hook/FileSystemHook.cpp
    Utils/VirtualSpoof.cpp
    Utils/HexDump.cpp
    Utils/AntiDetection.cpp
    Hook/VMClassLoaderHook.cpp
    Hook/UnixFileSystemHook.cpp
    Hook/BinderHook.cpp
    Hook/BaseHook.cpp
    Hook/ImGuiHook.cpp
    JniHook/JniHook.cpp
)

target_include_directories(blackbox PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_SOURCE_DIR}/Base ${CMAKE_CURRENT_SOURCE_DIR}/Jni ${CMAKE_CURRENT_SOURCE_DIR}/imgui ${CMAKE_CURRENT_SOURCE_DIR}/imgui_render ${CMAKE_CURRENT_SOURCE_DIR}/shadowhook/shadowhook/src/main/cpp/include)

target_compile_options(blackbox PRIVATE
    -Wno-error=format-security
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -w
    -std=c++17
    -fms-extensions
)

target_link_options(blackbox PRIVATE
    -Wl,--gc-sections,--strip-all,-z,max-page-size=16384
)

target_link_libraries(blackbox
    PRIVATE
    shadowhook
    xdl
    imgui_render
    log
    android
    EGL
    GLESv3
    z
)

