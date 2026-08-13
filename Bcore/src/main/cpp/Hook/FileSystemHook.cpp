#include "FileSystemHook.h"
#include "Log.h"
#include "shadowhook.h"
#include <sys/stat.h>
#include <fcntl.h>
#include <cstdarg>
#include <cstring>
#include <cerrno>

#ifndef O_TMPFILE
#define O_TMPFILE 020200000
#endif

namespace blackbox {

static int (*orig_open)(const char *pathname, int flags, ...) = nullptr;
static int (*orig_open64)(const char *pathname, int flags, ...) = nullptr;

static int new_open(const char *pathname, int flags, ...) {
    if (pathname != nullptr) {
        if (strstr(pathname, "resource-cache") || 
            strstr(pathname, "@idmap") || 
            strstr(pathname, ".frro") ||
            strstr(pathname, "systemui") ||
            strstr(pathname, "data@resource-cache@")) {
            ALOGD("FileSystemHook: Blocking problematic file access: %s", pathname);
            errno = ENOENT; 
            return -1;
        }
    }
    
    if ((flags & O_CREAT) || (flags & O_TMPFILE)) {
        va_list args;
        va_start(args, flags);
        mode_t mode = va_arg(args, mode_t);
        va_end(args);
        return orig_open(pathname, flags, mode);
    } else {
        return orig_open(pathname, flags);
    }
}

static int new_open64(const char *pathname, int flags, ...) {
    if (pathname != nullptr) {
        if (strstr(pathname, "resource-cache") || 
            strstr(pathname, "@idmap") || 
            strstr(pathname, ".frro") ||
            strstr(pathname, "systemui") ||
            strstr(pathname, "data@resource-cache@")) {
            ALOGD("FileSystemHook: Blocking problematic file access (64): %s", pathname);
            errno = ENOENT; 
            return -1;
        }
    }
    
    if ((flags & O_CREAT) || (flags & O_TMPFILE)) {
        va_list args;
        va_start(args, flags);
        mode_t mode = va_arg(args, mode_t);
        va_end(args);
        return orig_open64(pathname, flags, mode);
    } else {
        return orig_open64(pathname, flags);
    }
}

void FileSystemHook::init() {
    ALOGD("FileSystemHook: Initializing file system hooks");
    
    void* stub_open = shadowhook_hook_sym_name("libc.so", "open", (void*)new_open, (void**)&orig_open);
    if (stub_open) {
        ALOGD("FileSystemHook: Successfully hooked open function");
    } else {
        ALOGE("FileSystemHook: Failed to hook open function");
    }
    
    void* stub_open64 = shadowhook_hook_sym_name("libc.so", "open64", (void*)new_open64, (void**)&orig_open64);
    if (stub_open64) {
        ALOGD("FileSystemHook: Successfully hooked open64 function");
    } else {
        ALOGE("FileSystemHook: Failed to hook open64 function");
    }
}

} // namespace blackbox
