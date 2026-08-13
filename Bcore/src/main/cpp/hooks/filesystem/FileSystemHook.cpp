#include "hooks/filesystem/FileSystemHook.h"
#include "utils/Log.h"
#include "shadowhook.h"
#include "io/IO.h"
#include <sys/stat.h>
#include <fcntl.h>
#include <cstdarg>
#include <cstring>
#include <cstdlib>
#include <cerrno>

#ifndef O_TMPFILE
#define O_TMPFILE 020200000
#endif

namespace blackbox {

void FileSystemHook::init() {
    ALOGD("FileSystemHook: File system hooks active via AntiDetection");
}

} // namespace blackbox
