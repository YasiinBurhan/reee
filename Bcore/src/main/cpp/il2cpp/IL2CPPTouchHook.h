#ifndef IL2CPP_TOUCH_HOOK_H
#define IL2CPP_TOUCH_HOOK_H

#include "IL2CPPAPI.h"

class IL2CPPTouchHook {
public:
    // Initialize IL2CPP touch hooking to integrate ImGui touch input and block Unity touch passthrough
    static bool InstallHooks();
    
    // Check if IL2CPP touch hook is currently active
    static bool IsInstalled();
};

#endif // IL2CPP_TOUCH_HOOK_H
