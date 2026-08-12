#ifndef BLACKBOX_TEST_HARNESS_H
#define BLACKBOX_TEST_HARNESS_H

#include <jni.h>
#include <string>
#include <vector>

namespace blackbox {

struct TestResult {
    std::string name;
    std::string category;     // "UNIT", "INTEGRATION", "LIVE_HOOK"
    std::string staticStatus; // "PASS", "FAIL", "NOT_REVIEWED"
    std::string runtimeStatus;// "PASS", "FAIL", "SKIPPED", "NOT_EXECUTED", "UNSUPPORTED"
    std::string finalResult;  // "PASS", "FAIL", "PARTIAL", "NOT_VERIFIED", "UNSUPPORTED"
    std::string message;
};

class TestHarness {
public:
    static std::vector<TestResult> runAllDiagnostics(JNIEnv *env);
};

} // namespace blackbox

#endif // BLACKBOX_TEST_HARNESS_H
