#ifndef BCORE_JNI_UTILS_H
#define BCORE_JNI_UTILS_H

#include <jni.h>
#include "utils/Log.h"

namespace blackbox {

class ScopedUtfChars {
public:
    ScopedUtfChars(JNIEnv *env, jstring str) : env_(env), string_(str), chars_(nullptr) {
        if (env_ && string_) {
            chars_ = env_->GetStringUTFChars(string_, nullptr);
        }
    }

    ~ScopedUtfChars() {
        reset();
    }

    // Prevent copying
    ScopedUtfChars(const ScopedUtfChars&) = delete;
    ScopedUtfChars& operator=(const ScopedUtfChars&) = delete;

    // Enable moving
    ScopedUtfChars(ScopedUtfChars&& other) noexcept 
        : env_(other.env_), string_(other.string_), chars_(other.chars_) {
        other.chars_ = nullptr;
        other.string_ = nullptr;
    }

    ScopedUtfChars& operator=(ScopedUtfChars&& other) noexcept {
        if (this != &other) {
            reset();
            env_ = other.env_;
            string_ = other.string_;
            chars_ = other.chars_;
            other.chars_ = nullptr;
            other.string_ = nullptr;
        }
        return *this;
    }

    const char *c_str() const {
        return chars_ ? chars_ : "";
    }

    bool empty() const {
        return chars_ == nullptr;
    }

    void reset() {
        if (env_ && string_ && chars_) {
            env_->ReleaseStringUTFChars(string_, chars_);
            chars_ = nullptr;
        }
    }

private:
    JNIEnv *env_;
    jstring string_;
    const char *chars_;
};

template <typename T>
class ScopedLocalRef {
public:
    ScopedLocalRef(JNIEnv *env, T ref) : env_(env), ref_(ref) {}

    ~ScopedLocalRef() {
        reset();
    }

    // Prevent copying
    ScopedLocalRef(const ScopedLocalRef&) = delete;
    ScopedLocalRef& operator=(const ScopedLocalRef&) = delete;

    // Enable moving
    ScopedLocalRef(ScopedLocalRef&& other) noexcept : env_(other.env_), ref_(other.ref_) {
        other.ref_ = nullptr;
    }

    ScopedLocalRef& operator=(ScopedLocalRef&& other) noexcept {
        if (this != &other) {
            reset();
            env_ = other.env_;
            ref_ = other.ref_;
            other.ref_ = nullptr;
        }
        return *this;
    }

    T get() const {
        return ref_;
    }

    bool empty() const {
        return ref_ == nullptr;
    }

    operator T() const {
        return ref_;
    }

    void reset() {
        if (env_ && ref_) {
            env_->DeleteLocalRef(ref_);
            ref_ = nullptr;
        }
    }

private:
    JNIEnv *env_;
    T ref_;
};

class ScopedGlobalRef {
public:
    ScopedGlobalRef(JNIEnv *env, jobject ref) : env_(env), ref_(nullptr) {
        if (env && ref) {
            ref_ = env->NewGlobalRef(ref);
        }
    }

    ~ScopedGlobalRef() {
        reset();
    }

    // Prevent copying
    ScopedGlobalRef(const ScopedGlobalRef&) = delete;
    ScopedGlobalRef& operator=(const ScopedGlobalRef&) = delete;

    // Enable moving
    ScopedGlobalRef(ScopedGlobalRef&& other) noexcept : env_(other.env_), ref_(other.ref_) {
        other.ref_ = nullptr;
    }

    ScopedGlobalRef& operator=(ScopedGlobalRef&& other) noexcept {
        if (this != &other) {
            reset();
            env_ = other.env_;
            ref_ = other.ref_;
            other.ref_ = nullptr;
        }
        return *this;
    }

    jobject get() const {
        return ref_;
    }

    bool empty() const {
        return ref_ == nullptr;
    }

    void reset() {
        if (env_ && ref_) {
            env_->DeleteGlobalRef(ref_);
            ref_ = nullptr;
        }
    }

private:
    JNIEnv *env_;
    jobject ref_;
};

class AutoDetachEnv {
public:
    AutoDetachEnv(JavaVM *vm) : vm_(vm), env_(nullptr), attached_(false) {
        if (vm_) {
            jint res = vm_->GetEnv(reinterpret_cast<void**>(&env_), JNI_VERSION_1_6);
            if (res == JNI_EDETACHED) {
#ifdef __ANDROID__
                res = vm_->AttachCurrentThread(&env_, nullptr);
#else
                res = vm_->AttachCurrentThread(reinterpret_cast<void**>(&env_), nullptr);
#endif
                if (res == JNI_OK) {
                    attached_ = true;
                } else {
                    env_ = nullptr;
                }
            }
        }
    }

    ~AutoDetachEnv() {
        if (vm_ && attached_ && env_) {
            vm_->DetachCurrentThread();
        }
    }

    // Prevent copying
    AutoDetachEnv(const AutoDetachEnv&) = delete;
    AutoDetachEnv& operator=(const AutoDetachEnv&) = delete;

    JNIEnv* env() const {
        return env_;
    }

    bool attached() const {
        return attached_;
    }

private:
    JavaVM *vm_;
    JNIEnv *env_;
    bool attached_;
};

} // namespace blackbox

#endif // BCORE_JNI_UTILS_H
