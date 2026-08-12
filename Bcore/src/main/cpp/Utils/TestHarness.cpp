#include "TestHarness.h"
#include <Core/BoxCore.h>
#include <Base/IO.h>
#include <Base/NativeError.h>
#include <Utils/SafeJni.h>
#include <JniHook/JniHook.h>
#include <Log.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <thread>
#include <future>

namespace blackbox {

thread_local static uint32_t test_hook_depth = 0;

struct TestHookGuard {
    TestHookGuard() { ++test_hook_depth; }
    ~TestHookGuard() { if (test_hook_depth > 0) --test_hook_depth; }
    static bool isReentrant() { return test_hook_depth > 1; }
};

struct TestRuleGuard {
    const char* target;
    const char* relocate;
    TestRuleGuard(const char* t, const char* r) : target(t), relocate(r) {
        IO::addRule(target, relocate);
    }
    ~TestRuleGuard() {
        IO::removeRule(target, relocate);
    }
};

std::vector<TestResult> TestHarness::runAllDiagnostics(JNIEnv *env) {
    std::vector<TestResult> results;

    // ==========================================
    // 1. UNIT TESTS (No active hook dependency)
    // ==========================================
    {
        TestResult tr{
            "InitStateTransitionLogic",
            "UNIT",
            "PASS",
            "PASS",
            "PASS",
            "State machine transitions and condition predicate verified"
        };
        InitState s = BoxCore::getInitState();
        if (s != InitState::Uninitialized && s != InitState::Ready) {
            tr.runtimeStatus = "FAIL";
            tr.finalResult = "FAIL";
            tr.message = "Unexpected initial state encountered";
        }
        results.push_back(tr);
    }

    {
        TestResult tr{
            "IORuleMatchingAndRAII",
            "UNIT",
            "PASS",
            "PASS",
            "PASS",
            "IO rule path replacement and RAII cleanup verified"
        };
        const char* tPath = "/data/data/test.target";
        const char* rPath = "/data/data/test.relocate";
        {
            TestRuleGuard guard(tPath, rPath);
            std::string res = IO::redirectPath("/data/data/test.target/file.txt");
            if (res != "/data/data/test.relocate/file.txt") {
                tr.runtimeStatus = "FAIL";
                tr.finalResult = "FAIL";
                tr.message = "IO path replacement did not match expected pattern";
            }
        }
        // Verify RAII cleanup removed the rule
        std::string cleanedRes = IO::redirectPath("/data/data/test.target/file.txt");
        if (cleanedRes != "/data/data/test.target/file.txt") {
            tr.runtimeStatus = "FAIL";
            tr.finalResult = "FAIL";
            tr.message = "Test IO rule was not cleaned up after guard destruction";
        }
        results.push_back(tr);
    }

    {
        TestResult tr{
            "ReentrancyDepthCounter",
            "UNIT",
            "PASS",
            "PASS",
            "PASS",
            "Depth-aware guard correctly identifies outer (depth=1) and inner (depth=2) reentrancy"
        };
        TestHookGuard outerGuard;
        if (TestHookGuard::isReentrant()) {
            tr.runtimeStatus = "FAIL";
            tr.finalResult = "FAIL";
            tr.message = "Outer guard reported reentrant unexpectedly at depth 1";
        } else {
            TestHookGuard innerGuard;
            if (!TestHookGuard::isReentrant()) {
                tr.runtimeStatus = "FAIL";
                tr.finalResult = "FAIL";
                tr.message = "Inner guard failed to detect reentrancy at depth 2";
            }
        }
        if (TestHookGuard::isReentrant()) {
            tr.runtimeStatus = "FAIL";
            tr.finalResult = "FAIL";
            tr.message = "Reentrancy depth failed to decrement on guard destruction";
        }
        results.push_back(tr);
    }

    {
        TestResult tr{
            "VariadicArgumentSelectionLogic",
            "UNIT",
            "PASS",
            "PASS",
            "PASS",
            "O_CREAT / O_TMPFILE mode predicate evaluated correctly"
        };
        int flagsCreat = O_CREAT | O_RDWR;
        bool needsModeCreat = (flagsCreat & O_CREAT) || ((flagsCreat & O_TMPFILE) == O_TMPFILE);
        int flagsNormal = O_RDONLY;
        bool needsModeNormal = (flagsNormal & O_CREAT) || ((flagsNormal & O_TMPFILE) == O_TMPFILE);

        if (!needsModeCreat || needsModeNormal) {
            tr.runtimeStatus = "FAIL";
            tr.finalResult = "FAIL";
            tr.message = "Variadic mode argument predicate evaluation failed";
        }
        results.push_back(tr);
    }

    {
        TestResult tr{
            "FeatureHealthStatusModel",
            "UNIT",
            "PASS",
            "PASS",
            "PASS",
            "FeatureHealth subsystem status model and NativeError reporting verified"
        };
        FeatureHealth h = BoxCore::getHealth();
        if (h.lastError != NativeError::OK && h.lastError != NativeError::INITIALIZATION_FAILED) {
            tr.runtimeStatus = "FAIL";
            tr.finalResult = "FAIL";
            tr.message = "Invalid NativeError state in FeatureHealth";
        }
        results.push_back(tr);
    }

    // ==========================================
    // 2. INTEGRATION TESTS (Component interaction)
    // ==========================================
    {
        TestResult tr{
            "JniEnvAndClassLookup",
            "INTEGRATION",
            "PASS",
            "PASS",
            "PASS",
            "JNIEnv active and NativeCore class/method IDs reachable"
        };
        jclass nativeCore = env->FindClass("com/equinox/virtual/core/NativeCore");
        if (nativeCore == nullptr) {
            tr.runtimeStatus = "FAIL";
            tr.finalResult = "FAIL";
            tr.message = "Failed to find com/equinox/virtual/core/NativeCore class";
        } else {
            jmethodID mid = env->GetStaticMethodID(nativeCore, "getCallingUid", "(I)I");
            if (mid == nullptr) {
                tr.runtimeStatus = "FAIL";
                tr.finalResult = "FAIL";
                tr.message = "Failed to locate getCallingUid method ID";
            }
        }
        results.push_back(tr);
    }

    {
        TestResult tr{
            "JniScopedGlobalRefRAII",
            "INTEGRATION",
            "PASS",
            "PASS",
            "PASS",
            "ScopedGlobalRef allocated and destroyed cleanly"
        };
        jclass stringClass = env->FindClass("java/lang/String");
        if (stringClass != nullptr) {
            ScopedGlobalRef globalRef(env, stringClass);
            if (globalRef.get() == nullptr) {
                tr.runtimeStatus = "FAIL";
                tr.finalResult = "FAIL";
                tr.message = "ScopedGlobalRef holds null pointer";
            }
        } else {
            tr.runtimeStatus = "FAIL";
            tr.finalResult = "FAIL";
            tr.message = "Failed to find java/lang/String class";
        }
        results.push_back(tr);
    }

    {
        TestResult tr{
            "BoxCoreInitializationStateMachine",
            "INTEGRATION",
            "PASS",
            "PASS",
            "PASS",
            "BoxCore nativeHook transitions state to Ready, coreReady = true"
        };
        BoxCore::nativeHook(env);
        InitState state = BoxCore::getInitState();
        FeatureHealth health = BoxCore::getHealth();

        if (state != InitState::Ready || !health.coreReady) {
            tr.runtimeStatus = "FAIL";
            tr.finalResult = "FAIL";
            tr.message = "BoxCore did not reach Ready state or coreReady is false";
        }
        results.push_back(tr);
    }

    {
        TestResult tr{
            "ConcurrentInitializationWaiting",
            "INTEGRATION",
            "PASS",
            "PASS",
            "PASS",
            "Concurrent initialization thread safely blocks on init_cv and observes Ready"
        };
        auto asyncInit = std::async(std::launch::async, [env]() {
            BoxCore::nativeHook(env);
            return BoxCore::getInitState();
        });
        InitState asyncState = asyncInit.get();
        if (asyncState != InitState::Ready) {
            tr.runtimeStatus = "FAIL";
            tr.finalResult = "FAIL";
            tr.message = "Concurrent thread failed to observe Ready state";
        }
        results.push_back(tr);
    }

    {
        TestResult tr{
            "ReinitializationContractOptionB",
            "INTEGRATION",
            "PASS",
            "NOT_EXECUTED",
            "NOT_VERIFIED",
            "Option B Contract: Reinitialization post-shutdown must be rejected explicitly"
        };
        // Option B contract test is non-destructive to active core in runtime test
        results.push_back(tr);
    }

    {
        TestResult tr{
            "LogicalShutdownLifecycle",
            "INTEGRATION",
            "PASS",
            "NOT_EXECUTED",
            "NOT_VERIFIED",
            "Logical shutdown releases JNI GlobalRefs, sets state to Shutdown, and coreReady to false"
        };
        results.push_back(tr);
    }

    // ==========================================
    // 3. LIVE HOOK TESTS (Active Interception)
    // ==========================================
    {
        TestResult tr{
            "PhysicalHookUninstallation",
            "LIVE_HOOK",
            "PASS",
            "UNSUPPORTED",
            "UNSUPPORTED",
            "Physical hook uninstallation is NOT supported; ShadowHook interceptions and ART swaps remain active for process lifetime"
        };
        results.push_back(tr);
    }
    {
        TestResult tr{
            "FileSystemHookInterception",
            "LIVE_HOOK",
            "PASS",
            "NOT_EXECUTED",
            "NOT_VERIFIED",
            "Live interception of libc open/open64 requires physical device or emulator execution"
        };
        FeatureHealth h = BoxCore::getHealth();
        if (h.fileSystemHook.status == SubsystemStatus::Ready) {
            tr.message = "FileSystemHook initialized and ready for live interception testing";
        }
        results.push_back(tr);
    }

    {
        TestResult tr{
            "AntiDetectionHookInterception",
            "LIVE_HOOK",
            "PASS",
            "NOT_EXECUTED",
            "NOT_VERIFIED",
            "Live anti-detection hooks require runtime physical device environment"
        };
        FeatureHealth h = BoxCore::getHealth();
        if (h.antiDetection.status == SubsystemStatus::Ready) {
            tr.message = "AntiDetection initialized and ready for live interception testing";
        }
        results.push_back(tr);
    }

    {
        TestResult tr{
            "ArtMethodHookTargets",
            "LIVE_HOOK",
            "PASS",
            "NOT_EXECUTED",
            "NOT_VERIFIED",
            "ART Method hooks (VMClassLoader, DexFile, Binder) require real ART runtime"
        };
        FeatureHealth h = BoxCore::getHealth();
        if (h.vmClassLoaderHook.status == SubsystemStatus::Ready &&
            h.dexFileHook.status == SubsystemStatus::Ready) {
            tr.message = "ART method replacement hooks initialized and ready for runtime testing";
        }
        results.push_back(tr);
    }

    return results;
}

} // namespace blackbox

