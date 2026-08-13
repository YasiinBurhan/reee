#include "hooks/gui/ImGuiHook.h"
#include "third_party/imgui_render/ImGuiRenderEngine.h"

namespace blackbox {

void ImGuiHook::init(const char* packageName) {
    ::ImGuiRenderEngine::init(packageName);
}

void ImGuiHook::setEnabled(bool enabled) {
    ::ImGuiRenderEngine::setEnabled(enabled);
}

bool ImGuiHook::isEnabled() {
    return ::ImGuiRenderEngine::isEnabled();
}

} // namespace blackbox
