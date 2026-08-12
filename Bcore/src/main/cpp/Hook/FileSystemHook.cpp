#include "FileSystemHook.h"
#include "Log.h"
#include "shadowhook.h"
#include <sys/stat.h>
#include <fcntl.h>
#include <cstdarg>
#include <cstring>
#include <cerrno>

#include <Base/IO.h>

namespace blackbox {

static int (*orig_open)(const char *pathname, int flags, ...) = nullptr;
static int (*orig_open64)(const char *pathname, int flags, ...) = nullptr;

thread_local static uint32_t open_hook_depth = 0;

struct HookGuard {
    HookGuard() { ++open_hook_depth; }
    ~HookGuard() { if (open_hook_depth > 0) --open_hook_depth; }
    static bool isReentrant() { return open_hook_depth > 1; }
};

#ifndef O_TMPFILE
#define O_TMPFILE (__O_TMPFILE | O_DIRECTORY)
#endif

static int new_open(const char *pathname, int flags, ...) {
    HookGuard guard;
    if (pathname == nullptr || orig_open == nullptr) {
        return orig_open ? orig_open(pathname, flags) : -1;
    }

    bool needs_mode = (flags & O_CREAT) || ((flags & O_TMPFILE) == O_TMPFILE);
    mode_t mode = 0;
    if (needs_mode) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, mode_t);
        va_end(args);
    }

    if (guard.isReentrant()) {
        return needs_mode ? orig_open(pathname, flags, mode) : orig_open(pathname, flags);
    }

    std::string redirected = IO::redirectPath(pathname);
    const char* final_path = redirected.c_str();

    return needs_mode ? orig_open(final_path, flags, mode) : orig_open(final_path, flags);
}

static int new_open64(const char *pathname, int flags, ...) {
    HookGuard guard;
    if (pathname == nullptr || orig_open64 == nullptr) {
        return orig_open64 ? orig_open64(pathname, flags) : -1;
    }

    bool needs_mode = (flags & O_CREAT) || ((flags & O_TMPFILE) == O_TMPFILE);
    mode_t mode = 0;
    if (needs_mode) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, mode_t);
        va_end(args);
    }

    if (guard.isReentrant()) {
        return needs_mode ? orig_open64(pathname, flags, mode) : orig_open64(pathname, flags);
    }

    std::string redirected = IO::redirectPath(pathname);
    const char* final_path = redirected.c_str();

    return needs_mode ? orig_open64(final_path, flags, mode) : orig_open64(final_path, flags);
}

void FileSystemHook::init() {
    ALOGD("FileSystemHook: Initializing file system hooks via shadowhook");
    
    shadowhook_hook_sym_name("libc.so", "open", (void *)new_open, (void **)&orig_open);
    shadowhook_hook_sym_name("libc.so", "open64", (void *)new_open64, (void **)&orig_open64);
    
    ALOGD("FileSystemHook: Hooks installed");
}

} // namespace blackbox
