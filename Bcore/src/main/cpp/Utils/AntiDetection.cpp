#include <android/log.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <cstdio>
#include <cstring>
#include <cerrno>
#include <dirent.h>
#include "shadowhook.h"
#include "AntiDetection.h"

#define LOG_TAG "AntiDetection"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace blackbox {

struct SpoofedProp {
    const char* key;
    const char* value;
};

static int (*orig_system_property_get)(const char *name, char *value) = nullptr;

static const char* blocked_files[] = {
    "/system/xbin/su",
    "/system/bin/su",
    "/sbin/su",
    "/system/app/Superuser.apk",
    "/system/app/SuperSU.apk",
    "/system/etc/init.d/99SuperSUDaemon",
    "/system/xbin/daemonsu",
    "/system/xbin/sugote",
    "/system/bin/sugote-mksh",
    "/system/xbin/sugote-mksh",
    "/data/local/xbin/su",
    "/data/local/bin/su",
    "/data/local/tmp/su",
    "/system/bin/magisk",
    "/system/xbin/magisk",
    "/sbin/magisk",
    "/data/adb/magisk",
    "/data/virtual",
    "/data/data/com.benny.openlauncher",
    "/data/data/io.va.exposed",
    "/data/data/com.lody.virtual",
    "/data/data/com.excelliance.dualaid",
    "/data/data/com.lbe.parallel",
    "/data/data/com.dual.dualspace",
    "/data/data/com.ludashi.superboost",
    "/data/data/top.niunaijun.blackboxa",
    "/blackbox",
    "/virtual",
    "/dev/vboxguest",
    "/dev/vboxuser",
    "/dev/qemu_pipe",
    "/dev/goldfish_pipe",
    "/dev/socket/qemud",
    "/dev/socket/baseband_genyd",
    "/dev/socket/genyd",
    "/system/lib/libc_malloc_debug_qemu.so",
    "/sys/qemu_trace",
    "/system/bin/qemu-props",
    "/system/bin/nox-prop",
    "/sys/module/goldfish_audio",
    "/sys/module/goldfish_sync",
    "/proc/tty/drivers/goldfish",
    "/dev/goldfish_events",
    "/system/lib/libdroid4x.so",
    "/system/bin/windroyed",
    "/system/lib/libnoxspeedup.so",
    "/system/lib/libmemu.so",
    "/system/lib/libbluelog.so",
    "/system/xposed.prop",
    "/system/framework/XposedBridge.jar",
    "/data/data/de.robv.android.xposed.installer",
    "/data/data/org.meowcat.edxposed.manager",
    "/data/data/top.canyie.dreamland.manager",
    nullptr
};

static const char* blocked_packages[] = {
    "com.noshufou.android.su",
    "com.noshufou.android.su.elite", 
    "eu.chainfire.supersu",
    "com.koushikdutta.superuser",
    "com.thirdparty.superuser",
    "com.yellowes.su",
    "com.koushikdutta.rommanager",
    "com.koushikdutta.rommanager.license",
    "com.dimonvideo.luckypatcher",
    "com.chelpus.lackypatch",
    "com.ramdroid.appquarantine",
    "com.ramdroid.appquarantinepro",
    "com.devadvance.rootcloak",
    "com.devadvance.rootcloakplus",
    "de.robv.android.xposed.installer",
    "com.saurik.substrate",
    "com.zachspong.temprootremovejb",
    "com.amphoras.hidemyroot",
    "com.amphoras.hidemyrootadfree",
    "com.formyhm.hiderootPremium",
    "com.formyhm.hideroot",
    "me.phh.superuser",
    "eu.chainfire.supersu.pro",
    "com.kingouser.com",
    "com.topjohnwu.magisk",
    "com.lody.virtual",
    "io.va.exposed",
    "com.benny.openlauncher",
    nullptr
};

static bool is_blocked_file(const char* path) {
    if (!path) return false;
    for (int i = 0; blocked_files[i]; ++i) {
        if (strstr(path, blocked_files[i])) {
            return true;
        }
    }
    return false;
}

static bool is_blocked_package(const char* path) {
    if (!path) return false;
    for (int i = 0; blocked_packages[i]; ++i) {
        if (strstr(path, blocked_packages[i])) {
            return true;
        }
    }
    return false;
}

static int (*orig_access)(const char *pathname, int mode) = nullptr;
static int (*orig_stat)(const char *pathname, struct stat *buf) = nullptr;
static int (*orig_lstat)(const char *pathname, struct stat *buf) = nullptr;
static FILE* (*orig_fopen)(const char *pathname, const char *mode) = nullptr;
static int (*orig_open)(const char *pathname, int flags, ...) = nullptr;
static ssize_t (*orig_readlink)(const char *pathname, char *buf, size_t bufsiz) = nullptr;
static DIR* (*orig_opendir)(const char *name) = nullptr;

thread_local static uint32_t antidetection_hook_depth = 0;

struct AntiGuard {
    AntiGuard() { ++antidetection_hook_depth; }
    ~AntiGuard() { if (antidetection_hook_depth > 0) --antidetection_hook_depth; }
    static bool isReentrant() { return antidetection_hook_depth > 1; }
};

static bool is_safe_path(const char* path) {
    if (!path) return false;
    if (strstr(path, "/proc/net/")) return true;
    if (strstr(path, "/dev/socket/")) return true;
    return false;
}

static int my_access(const char *pathname, int mode) {
    AntiGuard guard;
    if (guard.isReentrant() || !orig_access) {
        return orig_access ? orig_access(pathname, mode) : -1;
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    return orig_access(pathname, mode);
}

static int my_stat(const char *pathname, struct stat *buf) {
    AntiGuard guard;
    if (guard.isReentrant() || !orig_stat) {
        return orig_stat ? orig_stat(pathname, buf) : -1;
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    return orig_stat(pathname, buf);
}

static int my_lstat(const char *pathname, struct stat *buf) {
    AntiGuard guard;
    if (guard.isReentrant() || !orig_lstat) {
        return orig_lstat ? orig_lstat(pathname, buf) : -1;
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    return orig_lstat(pathname, buf);
}

static FILE* my_fopen(const char *pathname, const char *mode) {
    AntiGuard guard;
    if (guard.isReentrant() || !orig_fopen) {
        return orig_fopen ? orig_fopen(pathname, mode) : nullptr;
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return nullptr;
    }
    return orig_fopen(pathname, mode);
}

static int my_open(const char *pathname, int flags, ...) {
    bool needs_mode = (flags & O_CREAT) || ((flags & O_TMPFILE) == O_TMPFILE);
    mode_t mode = 0;
    if (needs_mode) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, mode_t);
        va_end(args);
    }

    AntiGuard antiGuard;
    if (antiGuard.isReentrant() || !orig_open) {
        if (!orig_open) return -1;
        return needs_mode ? orig_open(pathname, flags, mode) : orig_open(pathname, flags);
    }

    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }

    return needs_mode ? orig_open(pathname, flags, mode) : orig_open(pathname, flags);
}

static ssize_t my_readlink(const char *pathname, char *buf, size_t bufsiz) {
    AntiGuard guard;
    if (guard.isReentrant() || !orig_readlink) {
        return orig_readlink ? orig_readlink(pathname, buf, bufsiz) : -1;
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    return orig_readlink(pathname, buf, bufsiz);
}

static DIR* my_opendir(const char *name) {
    AntiGuard guard;
    if (guard.isReentrant() || !orig_opendir) {
        return orig_opendir ? orig_opendir(name) : nullptr;
    }
    if (name && !is_safe_path(name) && (is_blocked_file(name) || is_blocked_package(name))) {
        errno = ENOENT;
        return nullptr;
    }
    return orig_opendir(name);
}

static void install_file_hooks() {
    LOGD("Installing file system hooks via shadowhook");
    
    shadowhook_hook_sym_name("libc.so", "access", (void *)my_access, (void **)&orig_access);
    shadowhook_hook_sym_name("libc.so", "stat", (void *)my_stat, (void **)&orig_stat);
    shadowhook_hook_sym_name("libc.so", "lstat", (void *)my_lstat, (void **)&orig_lstat);
    shadowhook_hook_sym_name("libc.so", "fopen", (void *)my_fopen, (void **)&orig_fopen);
    shadowhook_hook_sym_name("libc.so", "open", (void *)my_open, (void **)&orig_open);
    shadowhook_hook_sym_name("libc.so", "readlink", (void *)my_readlink, (void **)&orig_readlink);
    shadowhook_hook_sym_name("libc.so", "opendir", (void *)my_opendir, (void **)&orig_opendir);
    
    LOGD("File system hooks installed");
}

void AntiDetection::init() {
    LOGD("Installing anti-detection hooks...");
    install_file_hooks(); 
    LOGD("Anti-detection hooks installation complete");
}

} // namespace blackbox

__attribute__((constructor)) void install_antidetection_hooks() {
    blackbox::AntiDetection::init();
}
