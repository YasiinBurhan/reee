#include <android/log.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <cerrno>
#include <dirent.h>
#include <string>
#include <vector>
#include <mutex>
#include "shadowhook.h"
#include "security/AntiDetection.h"
#include "io/IO.h"

#define LOG_TAG "AntiDetection"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace blackbox {

static std::string g_target_package = "";
static std::mutex g_pkg_mutex;

void AntiDetection::setTargetPackage(const char* pkg) {
    if (!pkg) return;
    std::lock_guard<std::mutex> lock(g_pkg_mutex);
    g_target_package = pkg;
    LOGD("AntiDetection: Target package set to %s", pkg);
}

const char* AntiDetection::getTargetPackage() {
    std::lock_guard<std::mutex> lock(g_pkg_mutex);
    return g_target_package.c_str();
}

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
        const char* bf = blocked_files[i];
        if (strcmp(bf, "/virtual") == 0 || strcmp(bf, "/blackbox") == 0) {
            if (strcmp(path, "/virtual") == 0 || strcmp(path, "/virtual/") == 0 ||
                strcmp(path, "/blackbox") == 0 || strcmp(path, "/blackbox/") == 0) {
                return true;
            }
        } else if (strstr(path, bf)) {
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
static int (*orig_open64)(const char *pathname, int flags, ...) = nullptr;
static ssize_t (*orig_readlink)(const char *pathname, char *buf, size_t bufsiz) = nullptr;
static DIR* (*orig_opendir)(const char *name) = nullptr;
static int (*orig_system_property_get)(const char *name, char *value) = nullptr;

static bool is_problematic_resource_path(const char* pathname) {
    if (!pathname) return false;
    if (strstr(pathname, "resource-cache") || 
        strstr(pathname, "@idmap") || 
        strstr(pathname, ".frro") ||
        strstr(pathname, "systemui") ||
        strstr(pathname, "data@resource-cache@")) {
        return true;
    }
    return false;
}

static bool is_safe_path(const char* path) {
    if (!path) return false;
    if (strstr(path, "/proc/")) return true;
    if (strstr(path, "/dev/socket/")) return true;
    if (strstr(path, "com.equinox.virtual")) return true;
    if (strstr(path, "top.niunaijun.blackbox")) return true;
    if (strstr(path, "/blackbox/")) return true;
    if (strstr(path, "/virtual/")) return true;
    return false;
}

// -------------------------------------------------------------
// Procfs Maps & Status Masking
// -------------------------------------------------------------
static bool is_maps_path(const char* path) {
    if (!path) return false;
    return (strstr(path, "/maps") != nullptr || strstr(path, "/smaps") != nullptr) && strstr(path, "/proc/");
}

static bool is_status_path(const char* path) {
    if (!path) return false;
    return (strstr(path, "/status") != nullptr || strstr(path, "/wchan") != nullptr) && strstr(path, "/proc/");
}

static bool is_cmdline_path(const char* path) {
    if (!path) return false;
    return (strstr(path, "/cmdline") != nullptr) && strstr(path, "/proc/");
}

static bool should_filter_maps_line(const char* line) {
    if (!line) return false;
    if (strstr(line, "libblackbox") ||
        strstr(line, "shadowhook") ||
        strstr(line, "bytehook") ||
        strstr(line, "libimgui") ||
        strstr(line, "top.niunaijun.blackbox") ||
        strstr(line, "com.equinox.virtual") ||
        strstr(line, "xposed") ||
        strstr(line, "edxposed") ||
        strstr(line, "lsposed") ||
        strstr(line, "sandhook") ||
        strstr(line, "frida") ||
        strstr(line, "substrate")) {
        return true;
    }
    return false;
}

static FILE* create_filtered_maps_file() {
    FILE* real_fp = orig_fopen ? orig_fopen("/proc/self/maps", "r") : fopen("/proc/self/maps", "r");
    if (!real_fp) return nullptr;

    FILE* tmp_fp = tmpfile();
    if (!tmp_fp) {
        fclose(real_fp);
        return nullptr;
    }

    char buffer[1024];
    while (fgets(buffer, sizeof(buffer), real_fp)) {
        if (!should_filter_maps_line(buffer)) {
            fputs(buffer, tmp_fp);
        }
    }
    fclose(real_fp);
    rewind(tmp_fp);
    return tmp_fp;
}

static FILE* create_filtered_status_file() {
    FILE* real_fp = orig_fopen ? orig_fopen("/proc/self/status", "r") : fopen("/proc/self/status", "r");
    if (!real_fp) return nullptr;

    FILE* tmp_fp = tmpfile();
    if (!tmp_fp) {
        fclose(real_fp);
        return nullptr;
    }

    std::string target_pkg;
    {
        std::lock_guard<std::mutex> lock(g_pkg_mutex);
        target_pkg = g_target_package;
    }

    char buffer[1024];
    while (fgets(buffer, sizeof(buffer), real_fp)) {
        if (strncmp(buffer, "TracerPid:", 10) == 0) {
            // Mask ptrace detection
            fputs("TracerPid:\t0\n", tmp_fp);
        } else if (strncmp(buffer, "Name:", 5) == 0 && !target_pkg.empty()) {
            // Mask process name
            std::string short_name = target_pkg.substr(0, 15);
            fprintf(tmp_fp, "Name:\t%s\n", short_name.c_str());
        } else {
            fputs(buffer, tmp_fp);
        }
    }
    fclose(real_fp);
    rewind(tmp_fp);
    return tmp_fp;
}

static FILE* create_filtered_cmdline_file() {
    FILE* tmp_fp = tmpfile();
    if (!tmp_fp) return nullptr;

    std::string target_pkg;
    {
        std::lock_guard<std::mutex> lock(g_pkg_mutex);
        target_pkg = g_target_package;
    }

    if (target_pkg.empty()) {
        target_pkg = "com.android.system";
    }

    // Write null-terminated process name
    fwrite(target_pkg.c_str(), 1, target_pkg.length() + 1, tmp_fp);
    rewind(tmp_fp);
    return tmp_fp;
}

// -------------------------------------------------------------
// Interceptors
// -------------------------------------------------------------
static int my_access(const char *pathname, int mode) {
    if (pathname && is_problematic_resource_path(pathname)) {
        errno = ENOENT;
        return -1;
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    const char *redirected = pathname ? IO::redirectPath(pathname) : nullptr;
    const char *target = redirected ? redirected : pathname;
    int res = orig_access ? orig_access(target, mode) : -1;
    if (redirected && redirected != pathname) {
        free((void*)redirected);
    }
    return res;
}

static int my_stat(const char *pathname, struct stat *buf) {
    if (pathname && is_problematic_resource_path(pathname)) {
        errno = ENOENT;
        return -1;
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    const char *redirected = pathname ? IO::redirectPath(pathname) : nullptr;
    const char *target = redirected ? redirected : pathname;
    int res = orig_stat ? orig_stat(target, buf) : -1;
    if (redirected && redirected != pathname) {
        free((void*)redirected);
    }
    return res;
}

static int my_lstat(const char *pathname, struct stat *buf) {
    if (pathname && is_problematic_resource_path(pathname)) {
        errno = ENOENT;
        return -1;
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    const char *redirected = pathname ? IO::redirectPath(pathname) : nullptr;
    const char *target = redirected ? redirected : pathname;
    int res = orig_lstat ? orig_lstat(target, buf) : -1;
    if (redirected && redirected != pathname) {
        free((void*)redirected);
    }
    return res;
}

static FILE* my_fopen(const char *pathname, const char *mode) {
    if (pathname && is_problematic_resource_path(pathname)) {
        errno = ENOENT;
        return nullptr;
    }
    if (pathname && is_maps_path(pathname)) {
        return create_filtered_maps_file();
    }
    if (pathname && is_status_path(pathname)) {
        return create_filtered_status_file();
    }
    if (pathname && is_cmdline_path(pathname)) {
        return create_filtered_cmdline_file();
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return nullptr;
    }
    const char *redirected = pathname ? IO::redirectPath(pathname) : nullptr;
    const char *target = redirected ? redirected : pathname;
    FILE* res = orig_fopen ? orig_fopen(target, mode) : nullptr;
    if (redirected && redirected != pathname) {
        free((void*)redirected);
    }
    return res;
}

static int my_open(const char *pathname, int flags, ...) {
    if (pathname && is_problematic_resource_path(pathname)) {
        errno = ENOENT;
        return -1;
    }
    if (pathname && is_maps_path(pathname)) {
        FILE* fp = create_filtered_maps_file();
        return fp ? fileno(fp) : -1;
    }
    if (pathname && is_status_path(pathname)) {
        FILE* fp = create_filtered_status_file();
        return fp ? fileno(fp) : -1;
    }
    if (pathname && is_cmdline_path(pathname)) {
        FILE* fp = create_filtered_cmdline_file();
        return fp ? fileno(fp) : -1;
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    const char *redirected = pathname ? IO::redirectPath(pathname) : nullptr;
    const char *target = redirected ? redirected : pathname;
    int res = -1;
    if (orig_open) {
        if (flags & O_CREAT) {
            va_list args;
            va_start(args, flags);
            mode_t mode = (mode_t) va_arg(args, int);
            va_end(args);
            res = orig_open(target, flags, mode);
        } else {
            res = orig_open(target, flags);
        }
    }
    if (redirected && redirected != pathname) {
        free((void*)redirected);
    }
    return res;
}

static int my_open64(const char *pathname, int flags, ...) {
    if (pathname && is_problematic_resource_path(pathname)) {
        errno = ENOENT;
        return -1;
    }
    if (pathname && is_maps_path(pathname)) {
        FILE* fp = create_filtered_maps_file();
        return fp ? fileno(fp) : -1;
    }
    if (pathname && is_status_path(pathname)) {
        FILE* fp = create_filtered_status_file();
        return fp ? fileno(fp) : -1;
    }
    if (pathname && is_cmdline_path(pathname)) {
        FILE* fp = create_filtered_cmdline_file();
        return fp ? fileno(fp) : -1;
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    const char *redirected = pathname ? IO::redirectPath(pathname) : nullptr;
    const char *target = redirected ? redirected : pathname;
    int res = -1;
    if (orig_open64) {
        if (flags & O_CREAT) {
            va_list args;
            va_start(args, flags);
            mode_t mode = (mode_t) va_arg(args, int);
            va_end(args);
            res = orig_open64(target, flags, mode);
        } else {
            res = orig_open64(target, flags);
        }
    }
    if (redirected && redirected != pathname) {
        free((void*)redirected);
    }
    return res;
}

static ssize_t my_readlink(const char *pathname, char *buf, size_t bufsiz) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    const char *redirected = pathname ? IO::redirectPath(pathname) : nullptr;
    const char *target = redirected ? redirected : pathname;
    ssize_t res = orig_readlink ? orig_readlink(target, buf, bufsiz) : -1;
    if (redirected && redirected != pathname) {
        free((void*)redirected);
    }
    return res;
}

static DIR* my_opendir(const char *name) {
    if (name && !is_safe_path(name) && (is_blocked_file(name) || is_blocked_package(name))) {
        errno = ENOENT;
        return nullptr;
    }
    const char *redirected = name ? IO::redirectPath(name) : nullptr;
    const char *target = redirected ? redirected : name;
    DIR* res = orig_opendir ? orig_opendir(target) : nullptr;
    if (redirected && redirected != name) {
        free((void*)redirected);
    }
    return res;
}

static int my_system_property_get(const char *name, char *value) {
    if (!name || !value) return 0;

    if (strcmp(name, "ro.build.tags") == 0) {
        strcpy(value, "release-keys");
        return strlen(value);
    }
    if (strcmp(name, "ro.build.type") == 0) {
        strcpy(value, "user");
        return strlen(value);
    }
    if (strcmp(name, "ro.secure") == 0) {
        strcpy(value, "1");
        return strlen(value);
    }
    if (strcmp(name, "ro.debuggable") == 0) {
        strcpy(value, "0");
        return strlen(value);
    }
    if (strcmp(name, "service.adb.root") == 0) {
        strcpy(value, "0");
        return strlen(value);
    }
    if (strcmp(name, "init.svc.adbd") == 0) {
        strcpy(value, "stopped");
        return strlen(value);
    }

    return orig_system_property_get ? orig_system_property_get(name, value) : 0;
}

static void install_file_hooks() {
    LOGD("AntiDetection: Hooking file system and procfs detection functions...");
    
    shadowhook_hook_sym_name("libc.so", "access", (void*)my_access, (void**)&orig_access);
    shadowhook_hook_sym_name("libc.so", "stat", (void*)my_stat, (void**)&orig_stat);
    shadowhook_hook_sym_name("libc.so", "lstat", (void*)my_lstat, (void**)&orig_lstat);
    shadowhook_hook_sym_name("libc.so", "fopen", (void*)my_fopen, (void**)&orig_fopen);
    shadowhook_hook_sym_name("libc.so", "open", (void*)my_open, (void**)&orig_open);
    shadowhook_hook_sym_name("libc.so", "open64", (void*)my_open64, (void**)&orig_open64);
    shadowhook_hook_sym_name("libc.so", "readlink", (void*)my_readlink, (void**)&orig_readlink);
    shadowhook_hook_sym_name("libc.so", "opendir", (void*)my_opendir, (void**)&orig_opendir);
    shadowhook_hook_sym_name("libc.so", "__system_property_get", (void*)my_system_property_get, (void**)&orig_system_property_get);

    LOGD("Procfs & file security hooks installed via shadowhook");
}

void AntiDetection::init() {
    LOGD("Installing anti-detection hooks...");
    install_file_hooks(); 
    LOGD("Anti-detection hooks installation complete");
}

} // namespace blackbox

extern "C" JNIEXPORT void JNICALL Java_top_niunaijun_blackbox_core_IOCore_setTargetPackageNative(JNIEnv *env, jclass clazz, jstring packageName) {
    if (packageName == nullptr) return;
    const char *pkg = env->GetStringUTFChars(packageName, nullptr);
    if (pkg) {
        blackbox::AntiDetection::setTargetPackage(pkg);
        env->ReleaseStringUTFChars(packageName, pkg);
    }
}

__attribute__((constructor)) void install_antidetection_hooks() {
    blackbox::AntiDetection::init();
}
