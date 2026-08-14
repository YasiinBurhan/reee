#ifndef IMGUI_RENDER_ENGINE_H
#define IMGUI_RENDER_ENGINE_H

#include <string>

enum class ImGuiState {
    Disabled = 0,
    WaitingForGuest = 1,
    WaitingForSurface = 2,
    Initializing = 3,
    Ready = 4,
    ContextLost = 5,
    Failed = 6,
    Shutdown = 7
};

enum class GuestRenderState {
    NoGuest = 0,
    GuestStarting = 1,
    GuestRunning = 2,
    GuestStopping = 3
};

class ImGuiRenderEngine {
public:
    static void init(const char* packageName);
    static void setEnabled(bool enabled);
    static bool isEnabled();
    static void setGuestState(GuestRenderState state);
    static GuestRenderState getGuestState();
    static ImGuiState getState();
    static bool isVirtualGuestProcess();
    static void renderOverlay(int width, int height);
};

#endif // IMGUI_RENDER_ENGINE_H
