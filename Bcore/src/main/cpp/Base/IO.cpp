#include "IO.h"
#include "Log.h"
#include <Core/BoxCore.h>
#include <Base/JniUtils.h>
#include <cstring>
#include <list>
#include <mutex>

namespace blackbox {

static jmethodID getAbsolutePathMethodId;
static std::list<IO::RelocateInfo> relocate_rule;
static std::mutex relocate_rule_mutex;

static char *replace(const char *str, const char *src, const char *dst) {
    const char *pos = str;
    int count = 0;
    while ((pos = strstr(pos, src))) {
        count++;
        pos += strlen(src);
    }

    size_t result_len = strlen(str) + (strlen(dst) - strlen(src)) * count + 1;
    char *result = (char *) malloc(result_len);
    memset(result, 0, result_len);

    const char *left = str;
    const char *right = nullptr;

    while ((right = strstr(left, src))) {
        strncat(result, left, right - left);
        strcat(result, dst);
        right += strlen(src);
        left = right;
    }
    strcat(result, left);
    return result;
}

const char *IO::redirectPath(const char *__path) {
    if (strstr(__path, "resource-cache")) {
        ALOGD("Blocking resource-cache path: %s", __path);
        return "/dev/null";
    }
    
    if (strstr(__path, "@idmap")) {
        ALOGD("Blocking idmap path: %s", __path);
        return "/dev/null";
    }
    
    if (strstr(__path, "systemui") && (strstr(__path, ".frro") || strstr(__path, "-accent-") || strstr(__path, "-dynamic-") || strstr(__path, "-neutral-"))) {
        ALOGD("Blocking systemui problematic path: %s", __path);
        return "/dev/null";
    }
    
    if (strstr(__path, "data@resource-cache@")) {
        ALOGD("Blocking data@resource-cache@ pattern: %s", __path);
        return "/dev/null";
    }
    
    if (strstr(__path, ".frro")) {
        ALOGD("Blocking .frro file: %s", __path);
        return "/dev/null";
    }
    
    if (strstr(__path, "systemui")) {
        ALOGD("Blocking systemui path: %s", __path);
        return "/dev/null";
    }

    std::lock_guard<std::mutex> lock(relocate_rule_mutex);
    for (auto iterator = relocate_rule.begin(); iterator != relocate_rule.end(); ++iterator) {
        const IO::RelocateInfo &info = *iterator;
        if (strstr(__path, info.targetPath.c_str()) && !strstr(__path, "/blackbox/")) {
            char *ret = replace(__path, info.targetPath.c_str(), info.relocatePath.c_str());
            return ret;
        }
    }
    return __path;
}

jstring IO::redirectPath(JNIEnv *env, jstring path) {
    return BoxCore::redirectPathString(env, path);
}

jobject IO::redirectPath(JNIEnv *env, jobject path) {
    return BoxCore::redirectPathFile(env, path);
}

void IO::addRule(const char *targetPath, const char *relocatePath) {
    std::lock_guard<std::mutex> lock(relocate_rule_mutex);
    IO::RelocateInfo info{};
    info.targetPath = targetPath ? targetPath : "";
    info.relocatePath = relocatePath ? relocatePath : "";
    relocate_rule.push_back(info);
}

void IO::init(JNIEnv *env) {
    ScopedLocalRef<jclass> tmpFile(env, env->FindClass("java/io/File"));
    if (!tmpFile.empty()) {
        getAbsolutePathMethodId = env->GetMethodID(tmpFile.get(), "getAbsolutePath", "()Ljava/lang/String;");
    }
}

std::list<IO::RelocateInfo> IO::getRules() {
    std::lock_guard<std::mutex> lock(relocate_rule_mutex);
    return relocate_rule;
}

void IO::setRules(const std::list<IO::RelocateInfo> &rules) {
    std::lock_guard<std::mutex> lock(relocate_rule_mutex);
    relocate_rule = rules;
}

} // namespace blackbox
