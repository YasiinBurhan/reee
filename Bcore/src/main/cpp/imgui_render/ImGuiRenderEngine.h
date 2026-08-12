#ifndef IMGUI_RENDER_ENGINE_H
#define IMGUI_RENDER_ENGINE_H

#include <string>

class ImGuiRenderEngine {
public:
    static void init(const char* packageName);
    static void setEnabled(bool enabled);
    static bool isEnabled();
    static void renderOverlay(int width, int height);
};

#endif // IMGUI_RENDER_ENGINE_H
