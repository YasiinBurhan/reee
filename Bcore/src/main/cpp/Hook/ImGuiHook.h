#ifndef BLACKBOX_IMGUIHOOK_H
#define BLACKBOX_IMGUIHOOK_H

#include <jni.h>

class ImGuiHook {
public:
    static void init(const char* packageName);
    static void setEnabled(bool enabled);
    static bool isEnabled();
};

#endif // BLACKBOX_IMGUIHOOK_H
