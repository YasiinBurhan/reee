#ifndef BLACKBOX2_DEXFILEHOOK_H
#define BLACKBOX2_DEXFILEHOOK_H

#include "BaseHook.h"

namespace blackbox {

class DexFileHook : public BaseHook {
public:
    static void init(JNIEnv *env);
    static void setFileReadonly(const char* filePath);
};

} // namespace blackbox

#endif // BLACKBOX2_DEXFILEHOOK_H
