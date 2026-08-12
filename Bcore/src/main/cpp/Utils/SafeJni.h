#ifndef BCORE_SAFEJNI_H
#define BCORE_SAFEJNI_H

#include <jni.h>
#include <string>
#include <cstring>
#include <Core/BoxCore.h>

namespace blackbox {

class ScopedUtfChars {
public:
    ScopedUtfChars(JNIEnv* env, jstring s) : env_(env), string_(s), chars_(nullptr) {
        if (s != nullptr) {
            chars_ = env->GetStringUTFChars(s, nullptr);
        }
    }

    ~ScopedUtfChars() {
        if (chars_ != nullptr) {
            env_->ReleaseStringUTFChars(string_, chars_);
        }
    }

    const char* c_str() const {
        return chars_;
    }

    bool operator==(const char* s) const {
        return chars_ != nullptr && strcmp(chars_, s) == 0;
    }

private:
    JNIEnv* env_;
    jstring string_;
    const char* chars_;

    ScopedUtfChars(const ScopedUtfChars&) = delete;
    void operator=(const ScopedUtfChars&) = delete;
};

class ScopedLocalRef {
public:
    ScopedLocalRef(JNIEnv* env, jobject localRef) : env_(env), localRef_(localRef) {}

    ~ScopedLocalRef() {
        if (localRef_ != nullptr) {
            env_->DeleteLocalRef(localRef_);
        }
    }

    jobject get() const {
        return localRef_;
    }

private:
    JNIEnv* env_;
    jobject localRef_;

    ScopedLocalRef(const ScopedLocalRef&) = delete;
    void operator=(const ScopedLocalRef&) = delete;
};

class ScopedGlobalRef {
public:
    ScopedGlobalRef(JNIEnv* env, jobject localRef) : env_(env), globalRef_(nullptr) {
        if (localRef != nullptr) {
            globalRef_ = env->NewGlobalRef(localRef);
        }
    }

    ~ScopedGlobalRef() {
        if (globalRef_ != nullptr) {
            env_->DeleteGlobalRef(globalRef_);
        }
    }

    jobject get() const {
        return globalRef_;
    }

private:
    JNIEnv* env_;
    jobject globalRef_;

    ScopedGlobalRef(const ScopedGlobalRef&) = delete;
    void operator=(const ScopedGlobalRef&) = delete;
};

class ScopedByteArrayElements {
public:
    ScopedByteArrayElements(JNIEnv* env, jbyteArray array) : env_(env), array_(array), elements_(nullptr) {
        if (array != nullptr) {
            elements_ = env->GetByteArrayElements(array, nullptr);
        }
    }

    ~ScopedByteArrayElements() {
        if (elements_ != nullptr) {
            env_->ReleaseByteArrayElements(array_, elements_, 0);
        }
    }

    jbyte* get() const { return elements_; }

private:
    JNIEnv* env_;
    jbyteArray array_;
    jbyte* elements_;

    ScopedByteArrayElements(const ScopedByteArrayElements&) = delete;
    ScopedByteArrayElements& operator=(const ScopedByteArrayElements&) = delete;
};

class ScopedIntArrayElements {
public:
    ScopedIntArrayElements(JNIEnv* env, jintArray array) : env_(env), array_(array), elements_(nullptr) {
        if (array != nullptr) {
            elements_ = env->GetIntArrayElements(array, nullptr);
        }
    }

    ~ScopedIntArrayElements() {
        if (elements_ != nullptr) {
            env_->ReleaseIntArrayElements(array_, elements_, 0);
        }
    }

    jint* get() const { return elements_; }

private:
    JNIEnv* env_;
    jintArray array_;
    jint* elements_;

    ScopedIntArrayElements(const ScopedIntArrayElements&) = delete;
    ScopedIntArrayElements& operator=(const ScopedIntArrayElements&) = delete;
};

class ScopedLongArrayElements {
public:
    ScopedLongArrayElements(JNIEnv* env, jlongArray array) : env_(env), array_(array), elements_(nullptr) {
        if (array != nullptr) {
            elements_ = env->GetLongArrayElements(array, nullptr);
        }
    }

    ~ScopedLongArrayElements() {
        if (elements_ != nullptr) {
            env_->ReleaseLongArrayElements(array_, elements_, 0);
        }
    }

    jlong* get() const { return elements_; }

private:
    JNIEnv* env_;
    jlongArray array_;
    jlong* elements_;

    ScopedLongArrayElements(const ScopedLongArrayElements&) = delete;
    ScopedLongArrayElements& operator=(const ScopedLongArrayElements&) = delete;
};

class AutoDetachEnv {
public:
    AutoDetachEnv(JavaVM* vm) : vm_(vm), env_(nullptr), needDetach_(false) {
        if (vm_ != nullptr) {
            int status = vm_->GetEnv(reinterpret_cast<void**>(&env_), JNI_VERSION_1_6);
            if (status == JNI_EDETACHED) {
                if (vm_->AttachCurrentThread(&env_, nullptr) == JNI_OK) {
                    needDetach_ = true;
                }
            }
        }
    }

    JNIEnv* get() const { return env_; }

    ~AutoDetachEnv() {
        if (needDetach_ && vm_ != nullptr) {
            vm_->DetachCurrentThread();
        }
    }

private:
    JavaVM* vm_;
    JNIEnv* env_;
    bool needDetach_;

    AutoDetachEnv(const AutoDetachEnv&) = delete;
    AutoDetachEnv& operator=(const AutoDetachEnv&) = delete;
};

} // namespace blackbox

#endif // BCORE_SAFEJNI_H
