#ifndef IMGUI_MENU_UI_H
#define IMGUI_MENU_UI_H

#include <string>

class ImGuiMenuUI {
public:
    static void renderOverlay(int width, int height, const std::string& targetPackage);
};

#endif // IMGUI_MENU_UI_H
