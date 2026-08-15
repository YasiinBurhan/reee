#include "io/IO.h"
#include "utils/Log.h"
#include "core/BoxCore.h"
#include "utils/JniUtils.h"
#include <cstring>
#include <list>
#include <mutex>

namespace blackbox {

static jmethodID getAbsolutePathMethodId;
static std::list<IO::RelocateInfo> relocate_rule;
static std::mutex relocate_rule_mutex;

static char *replace(const char *str, const char *src, const char *dst) {
    if (!str || !src || !dst) {
        return nullptr;
    }
    size_t src_len = strlen(src);
    if (src_len == 0) {
        return nullptr;
    }
    size_t dst_len = strlen(dst);

    const char *pos = str;
    int count = 0;
    while ((pos = strstr(pos, src))) {
        count++;
        pos += src_len;
    }

    if (count == 0) {
        return nullptr;
    }

    size_t result_len = strlen(str) + (dst_len - src_len) * count + 1;
    char *result = (char *) malloc(result_len);
    if (!result) return nullptr;
    memset(result, 0, result_len);

    const char *left = str;
    const char *right = nullptr;

    while ((right = strstr(left, src))) {
        strncat(result, left, right - left);
        strcat(result, dst);
        left = right + src_len;
    }
    strcat(result, left);
    return result;
}

const char *IO::redirectPath(const char *__path) {
    if (!__path || strlen(__path) == 0) return nullptr;

    std::lock_guard<std::mutex> lock(relocate_rule_mutex);
    for (auto iterator = relocate_rule.begin(); iterator != relocate_rule.end(); ++iterator) {
        const IO::RelocateInfo &info = *iterator;
        if (info.targetPath.empty() || info.relocatePath.empty()) continue;
        if (strstr(__path, info.targetPath.c_str()) && !strstr(__path, "/blackbox/")) {
            char *ret = replace(__path, info.targetPath.c_str(), info.relocatePath.c_str());
            if (ret) {
                return ret;
            }
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
    if (!targetPath || strlen(targetPath) == 0 || !relocatePath || strlen(relocatePath) == 0) {
        return;
    }
    std::lock_guard<std::mutex> lock(relocate_rule_mutex);
    IO::RelocateInfo info{};
    info.targetPath = targetPath;
    info.relocatePath = relocatePath;
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
