#include "ImGuiMenuUI.h"
#include "imgui.h"
#include <unistd.h>

static bool g_MenuOpen = true;
static int g_ActiveTab = 0;
static float g_MenuAlpha = 0.90f;
static int g_SelectedTheme = 0;

static bool g_SpeedHack = false;
static float g_SpeedMultiplier = 1.0f;
static bool g_SpoofAndroidId = false;
static bool g_BypassIntegrity = false;
static bool g_AntiCheatBypass = false;

static bool g_ShowFPS = true;
static bool g_ShowCrosshair = false;
static int g_CrosshairStyle = 0;
static float g_CrosshairSize = 10.0f;
static float g_CrosshairColor[4] = { 1.0f, 0.0f, 0.0f, 1.0f };

static ImVec2 g_FloatingPos = ImVec2(50.0f, 150.0f);
static bool g_FloatingInitialized = false;

static void ApplyTheme(int theme_id) {
    ImGuiStyle& style = ImGui::GetStyle();
    ImVec4* colors = style.Colors;

    style.WindowRounding = 12.0f;
    style.FrameRounding = 8.0f;
    style.PopupRounding = 8.0f;
    style.ScrollbarRounding = 12.0f;
    style.GrabRounding = 6.0f;
    style.WindowBorderSize = 1.5f;
    style.FrameBorderSize = 1.0f;

    ImVec4 primaryColor;
    ImVec4 primaryHovered;
    ImVec4 primaryActive;

    if (theme_id == 0) { // Neon Purple
        primaryColor = ImVec4(0.57f, 0.23f, 0.85f, 1.0f);
        primaryHovered = ImVec4(0.67f, 0.33f, 0.95f, 1.0f);
        primaryActive = ImVec4(0.47f, 0.13f, 0.75f, 1.0f);
    } else if (theme_id == 1) { // Emerald Green
        primaryColor = ImVec4(0.00f, 0.78f, 0.45f, 1.0f);
        primaryHovered = ImVec4(0.10f, 0.88f, 0.55f, 1.0f);
        primaryActive = ImVec4(0.00f, 0.68f, 0.35f, 1.0f);
    } else if (theme_id == 2) { // Cyberpunk Yellow
        primaryColor = ImVec4(0.98f, 0.85f, 0.08f, 1.0f);
        primaryHovered = ImVec4(1.00f, 0.90f, 0.20f, 1.0f);
        primaryActive = ImVec4(0.88f, 0.75f, 0.00f, 1.0f);
    } else { // Classic Dark / Steel Blue
        primaryColor = ImVec4(0.18f, 0.48f, 0.96f, 1.0f);
        primaryHovered = ImVec4(0.28f, 0.58f, 0.99f, 1.0f);
        primaryActive = ImVec4(0.08f, 0.38f, 0.86f, 1.0f);
    }

    colors[ImGuiCol_Text]                   = ImVec4(0.95f, 0.96f, 0.98f, 1.0f);
    colors[ImGuiCol_TextDisabled]           = ImVec4(0.50f, 0.50f, 0.50f, 1.0f);
    colors[ImGuiCol_WindowBg]               = ImVec4(0.06f, 0.05f, 0.08f, g_MenuAlpha);
    colors[ImGuiCol_ChildBg]                = ImVec4(0.10f, 0.09f, 0.12f, 0.50f);
    colors[ImGuiCol_PopupBg]                = ImVec4(0.10f, 0.09f, 0.15f, 0.98f);
    colors[ImGuiCol_Border]                 = primaryColor;
    colors[ImGuiCol_BorderShadow]           = ImVec4(0.00f, 0.00f, 0.00f, 0.00f);
    colors[ImGuiCol_FrameBg]                = ImVec4(0.14f, 0.13f, 0.17f, 1.0f);
    colors[ImGuiCol_FrameBgHovered]         = ImVec4(0.20f, 0.19f, 0.24f, 1.0f);
    colors[ImGuiCol_FrameBgActive]          = ImVec4(0.26f, 0.25f, 0.30f, 1.0f);
    colors[ImGuiCol_TitleBg]                = ImVec4(0.08f, 0.07f, 0.10f, 1.0f);
    colors[ImGuiCol_TitleBgActive]          = ImVec4(0.12f, 0.11f, 0.15f, 1.0f);
    colors[ImGuiCol_TitleBgCollapsed]       = ImVec4(0.00f, 0.00f, 0.00f, 0.51f);
    colors[ImGuiCol_MenuBarBg]              = ImVec4(0.14f, 0.14f, 0.14f, 1.0f);
    colors[ImGuiCol_ScrollbarBg]            = ImVec4(0.02f, 0.02f, 0.02f, 0.39f);
    colors[ImGuiCol_ScrollbarGrab]          = ImVec4(0.31f, 0.31f, 0.31f, 1.0f);
    colors[ImGuiCol_ScrollbarGrabHovered]   = ImVec4(0.41f, 0.41f, 0.41f, 1.0f);
    colors[ImGuiCol_ScrollbarGrabActive]    = ImVec4(0.51f, 0.51f, 0.51f, 1.0f);
    colors[ImGuiCol_CheckMark]              = primaryColor;
    colors[ImGuiCol_SliderGrab]             = primaryColor;
    colors[ImGuiCol_SliderGrabActive]       = primaryActive;
    colors[ImGuiCol_Button]                 = primaryColor;
    colors[ImGuiCol_ButtonHovered]          = primaryHovered;
    colors[ImGuiCol_ButtonActive]           = primaryActive;
    colors[ImGuiCol_Header]                 = ImVec4(primaryColor.x, primaryColor.y, primaryColor.z, 0.40f);
    colors[ImGuiCol_HeaderHovered]          = ImVec4(primaryColor.x, primaryColor.y, primaryColor.z, 0.65f);
    colors[ImGuiCol_HeaderActive]           = primaryColor;
    colors[ImGuiCol_Separator]              = ImVec4(0.20f, 0.19f, 0.24f, 1.0f);
    colors[ImGuiCol_SeparatorHovered]       = primaryHovered;
    colors[ImGuiCol_SeparatorActive]        = primaryActive;
}

void ImGuiMenuUI::renderOverlay(int width, int height, const std::string& targetPackage) {
    if (!g_FloatingInitialized) {
        g_FloatingPos = ImVec2(40.0f, (float)height * 0.3f);
        g_FloatingInitialized = true;
    }

    if (!g_MenuOpen) {
        ImGui::SetNextWindowSize(ImVec2(75, 75));
        ImGui::SetNextWindowPos(g_FloatingPos, ImGuiCond_Always);
        
        ImGui::PushStyleColor(ImGuiCol_WindowBg, ImVec4(0.08f, 0.07f, 0.10f, 0.85f));
        ImGui::PushStyleColor(ImGuiCol_Border, ImVec4(0.57f, 0.23f, 0.85f, 1.0f));
        ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(4, 4));
        ImGui::PushStyleVar(ImGuiStyleVar_WindowRounding, 37.5f);
        ImGui::PushStyleVar(ImGuiStyleVar_WindowMinSize, ImVec2(10, 10));

        ImGui::Begin("FloatingMenuButton", nullptr, 
            ImGuiWindowFlags_NoDecoration | 
            ImGuiWindowFlags_NoScrollWithMouse | 
            ImGuiWindowFlags_NoResize | 
            ImGuiWindowFlags_NoSavedSettings | 
            ImGuiWindowFlags_NoFocusOnAppearing | 
            ImGuiWindowFlags_NoBringToFrontOnFocus);

        if (ImGui::IsWindowFocused() && ImGui::IsMouseDragging(0)) {
            g_FloatingPos.x += ImGui::GetIO().MouseDelta.x;
            g_FloatingPos.y += ImGui::GetIO().MouseDelta.y;
            
            if (g_FloatingPos.x < 0) g_FloatingPos.x = 0;
            if (g_FloatingPos.y < 0) g_FloatingPos.y = 0;
            if (g_FloatingPos.x > (float)width - 75) g_FloatingPos.x = (float)width - 75;
            if (g_FloatingPos.y > (float)height - 75) g_FloatingPos.y = (float)height - 75;
        }

        ImGui::SetCursorPos(ImVec2(12.5f, 12.5f));
        if (ImGui::Button("EQ", ImVec2(50.0f, 50.0f))) {
            g_MenuOpen = true;
        }
        
        ImGui::End();
        ImGui::PopStyleVar(3);
        ImGui::PopStyleColor(2);
    } else {
        ApplyTheme(g_SelectedTheme);

        ImGui::SetNextWindowSize(ImVec2(450, 320), ImGuiCond_FirstUseEver);
        ImGui::Begin("EQuinox Virtual Menu", nullptr, ImGuiWindowFlags_NoCollapse);

        if (ImGui::Button("Minimize Menu", ImVec2(120, 24))) {
            g_MenuOpen = false;
        }
        ImGui::SameLine();
        ImGui::TextColored(ImVec4(1.0f, 1.0f, 1.0f, 0.5f), "| Package: %s", targetPackage.empty() ? "com.virtual.cloned" : targetPackage.c_str());

        ImGui::Separator();

        if (ImGui::Button("MAIN", ImVec2(90, 30))) g_ActiveTab = 0;
        ImGui::SameLine();
        if (ImGui::Button("VISUALS", ImVec2(90, 30))) g_ActiveTab = 1;
        ImGui::SameLine();
        if (ImGui::Button("SETTINGS", ImVec2(90, 30))) g_ActiveTab = 2;
        ImGui::SameLine();
        if (ImGui::Button("SYSTEM", ImVec2(90, 30))) g_ActiveTab = 3;

        ImGui::Separator();

        switch (g_ActiveTab) {
            case 0: { // MAIN
                ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.7f, 1.0f), "Core Hacks & Overrides");
                ImGui::Dummy(ImVec2(0.0f, 4.0f));

                ImGui::Checkbox("Virtual Speed Hack Emulator", &g_SpeedHack);
                if (g_SpeedHack) {
                    ImGui::SliderFloat("Multiplier", &g_SpeedMultiplier, 0.1f, 10.0f, "%.1fx");
                }
                
                ImGui::Checkbox("Spoof Android Device ID", &g_SpoofAndroidId);
                ImGui::Checkbox("Bypass Security Integrity checks", &g_BypassIntegrity);
                ImGui::Checkbox("Bypass Third-Party Anti-Cheat", &g_AntiCheatBypass);
                break;
            }
            case 1: { // VISUALS
                ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.7f, 1.0f), "Visual Overlays");
                ImGui::Dummy(ImVec2(0.0f, 4.0f));

                ImGui::Checkbox("Show Performance FPS Widget", &g_ShowFPS);
                ImGui::Checkbox("Show Hardware Crosshair", &g_ShowCrosshair);
                
                if (g_ShowCrosshair) {
                    ImGui::Combo("Style", &g_CrosshairStyle, "Dot\0Plus\0Circle\0");
                    ImGui::SliderFloat("Size", &g_CrosshairSize, 5.0f, 30.0f, "%.0fpx");
                    ImGui::ColorEdit4("Crosshair Color", g_CrosshairColor);
                }
                break;
            }
            case 2: { // SETTINGS
                ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.7f, 1.0f), "UI & Menu Customization");
                ImGui::Dummy(ImVec2(0.0f, 4.0f));

                ImGui::Combo("Theme Accent", &g_SelectedTheme, "Neon Purple\0Emerald Green\0Cyberpunk Yellow\0Steel Blue\0");
                ImGui::SliderFloat("Menu Transparency", &g_MenuAlpha, 0.40f, 1.00f, "%.2f");
                break;
            }
            case 3: { // SYSTEM
                ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.7f, 1.0f), "Sandbox Information");
                ImGui::Dummy(ImVec2(0.0f, 4.0f));

                ImGui::Text("Sandbox Engine: EQuinox Android Virtual Engine");
                ImGui::Text("Render Backend: OpenGL ES 3.0 / EGL");
                ImGui::Text("Current Process ID: %d", getpid());
                ImGui::Text("Active Core FPS: %.1f FPS", ImGui::GetIO().Framerate);
                ImGui::Text("Container Screen Size: %d x %d", width, height);
                ImGui::Text("Status: Sandbox Integrity verified (Anti-Detect OK)");
                break;
            }
        }

        ImGui::End();
    }

    if (g_ShowFPS) {
        ImGui::SetNextWindowBgAlpha(0.35f);
        ImGui::SetNextWindowPos(ImVec2((float)width - 100, 10), ImGuiCond_Always);
        ImGui::Begin("FPSOverlay", nullptr, ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_AlwaysAutoResize | ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoFocusOnAppearing | ImGuiWindowFlags_NoNav);
        ImGui::TextColored(ImVec4(0.0f, 1.0f, 0.5f, 1.0f), "FPS: %.1f", ImGui::GetIO().Framerate);
        ImGui::End();
    }

    if (g_ShowCrosshair) {
        ImDrawList* draw_list = ImGui::GetForegroundDrawList();
        float cx = (float)width / 2.0f;
        float cy = (float)height / 2.0f;
        ImU32 color = ImGui::ColorConvertFloat4ToU32(ImVec4(g_CrosshairColor[0], g_CrosshairColor[1], g_CrosshairColor[2], g_CrosshairColor[3]));

        if (g_CrosshairStyle == 0) { // Dot
            draw_list->AddCircleFilled(ImVec2(cx, cy), g_CrosshairSize / 4.0f, color);
        } else if (g_CrosshairStyle == 1) { // Plus
            draw_list->AddLine(ImVec2(cx - g_CrosshairSize, cy), ImVec2(cx + g_CrosshairSize, cy), color, 2.0f);
            draw_list->AddLine(ImVec2(cx, cy - g_CrosshairSize), ImVec2(cx, cy + g_CrosshairSize), color, 2.0f);
        } else if (g_CrosshairStyle == 2) { // Circle
            draw_list->AddCircle(ImVec2(cx, cy), g_CrosshairSize, color, 24, 2.0f);
        }
    }
}
