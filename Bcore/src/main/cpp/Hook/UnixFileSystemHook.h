#ifndef VIRTUALM_UNIXFILESYSTEMHOOK_H
#define VIRTUALM_UNIXFILESYSTEMHOOK_H

#include "BaseHook.h"

namespace blackbox {

class UnixFileSystemHook : public BaseHook {
public:
    static void init(JNIEnv *env);
};

} // namespace blackbox

#endif // VIRTUALM_UNIXFILESYSTEMHOOK_H
