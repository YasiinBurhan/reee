#include "ImGuiHook.h"
#include "../imgui_render/ImGuiRenderEngine.h"

void ImGuiHook::init(const char* packageName) {
    ImGuiRenderEngine::init(packageName);
}

void ImGuiHook::setEnabled(bool enabled) {
    ImGuiRenderEngine::setEnabled(enabled);
}

bool ImGuiHook::isEnabled() {
    return ImGuiRenderEngine::isEnabled();
}

