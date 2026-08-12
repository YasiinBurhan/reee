#include "IO.h"
#include "Log.h"
#include <Core/BoxCore.h>
#include <cstring>
#include <list>

namespace blackbox {

static jmethodID getAbsolutePathMethodId;
static std::list<IO::RelocateInfo> relocate_rule;
static std::mutex relocate_mutex;

static std::string replace_all(std::string str, const std::string& from, const std::string& to) {
    size_t start_pos = 0;
    while ((start_pos = str.find(from, start_pos)) != std::string::npos) {
        str.replace(start_pos, from.length(), to);
        start_pos += to.length();
    }
    return str;
}

std::string IO::redirectPath(const std::string& path) {
    if (path.empty()) return path;

    const char* __path = path.c_str();
    if (strstr(__path, "resource-cache") || 
        strstr(__path, "@idmap") ||
        strstr(__path, ".frro") ||
        strstr(__path, "systemui") ||
        strstr(__path, "data@resource-cache@")) {
        return "/dev/null";
    }

    std::lock_guard<std::mutex> lock(relocate_mutex);
    for (const auto& info : relocate_rule) {
        if (path.find(info.targetPath) == 0) { // Starts with
             if (path.find("/blackbox/") == std::string::npos) {
                return replace_all(path, info.targetPath, info.relocatePath);
             }
        }
    }
    return path;
}

jstring IO::redirectPath(JNIEnv *env, jstring path) {
    return BoxCore::redirectPathString(env, path);
}

jobject IO::redirectPath(JNIEnv *env, jobject path) {
    return BoxCore::redirectPathFile(env, path);
}

void IO::addRule(const char *targetPath, const char *relocatePath) {
    if (targetPath == nullptr || relocatePath == nullptr) return;
    
    std::lock_guard<std::mutex> lock(relocate_mutex);
    IO::RelocateInfo info{};
    info.targetPath = targetPath;
    info.relocatePath = relocatePath;
    relocate_rule.push_back(info);
}

void IO::removeRule(const char *targetPath, const char *relocatePath) {
    if (targetPath == nullptr || relocatePath == nullptr) return;

    std::lock_guard<std::mutex> lock(relocate_mutex);
    relocate_rule.remove_if([targetPath, relocatePath](const IO::RelocateInfo& info) {
        return info.targetPath == targetPath && info.relocatePath == relocatePath;
    });
}

void IO::init(JNIEnv *env) {
    jclass tmpFile = env->FindClass("java/io/File");
    getAbsolutePathMethodId = env->GetMethodID(tmpFile, "getAbsolutePath", "()Ljava/lang/String;");
}

} // namespace blackbox
