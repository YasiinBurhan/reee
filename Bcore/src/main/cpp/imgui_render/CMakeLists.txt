cmake_minimum_required(VERSION 3.22.1)

add_library(imgui_render STATIC
    ImGuiRenderEngine.cpp
    ../imgui/imgui.cpp
    ../imgui/imgui_draw.cpp
    ../imgui/imgui_widgets.cpp
    ../imgui/imgui_tables.cpp
    ../imgui/imgui_demo.cpp
    ../imgui/backends/imgui_impl_opengl3.cpp
    ../imgui/backends/imgui_impl_android.cpp
)

target_include_directories(imgui_render PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/../imgui
    ${CMAKE_CURRENT_SOURCE_DIR}/../Base
)

target_compile_options(imgui_render PRIVATE
    -w
    -std=c++17
    -fms-extensions
)

target_link_libraries(imgui_render
    PUBLIC
    bytehook::bytehook
    log
    android
    EGL
    GLESv3
)

