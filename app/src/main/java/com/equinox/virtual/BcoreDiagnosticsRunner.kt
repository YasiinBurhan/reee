package com.equinox.virtual

import android.os.Build
import android.os.Process
import android.util.Log
import com.equinox.virtual.core.NativeCore

object BcoreDiagnosticsRunner {

    private const val TAG = "BCORE-RUNTIME"

    data class DiagnosticRunResult(
        val success: Boolean,
        val logs: List<String>,
        val errorMessage: String? = null
    )

    fun runDiagnostics(): DiagnosticRunResult {
        val logs = mutableListOf<String>()

        fun logLine(msg: String) {
            Log.i(TAG, msg)
            logs.add(msg)
        }

        fun logError(msg: String, t: Throwable? = null) {
            if (t != null) {
                Log.e(TAG, msg, t)
                logs.add("$msg | Exception: ${t.javaClass.simpleName} - ${t.message}")
            } else {
                Log.e(TAG, msg)
                logs.add(msg)
            }
        }

        logLine("==================================================")
        logLine("[ENV]")
        logLine("Android Version: ${Build.VERSION.RELEASE}")
        logLine("API Level: ${Build.VERSION.SDK_INT}")
        logLine("ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        logLine("Process Architecture: ${if (Process.is64Bit()) "64-bit" else "32-bit"}")
        logLine("Build Variant: ${if (BuildConfig.DEBUG) "debug" else "release"}")
        logLine("BCORE_DIAGNOSTICS: ${if (BuildConfig.DEBUG) "1" else "0"}")
        logLine("==================================================")

        logLine("[NATIVE]")
        try {
            logLine("Library Load: Attempting System.loadLibrary(\"blackbox\")...")
            System.loadLibrary("blackbox")
            logLine("Library Load: SUCCESS")
            logLine("JNI_OnLoad Status: Executed successfully")
            logLine("NativeCore Availability: Bound and available")
        } catch (t: Throwable) {
            logError("[FAILURE] Native Library Load Failed", t)
            return DiagnosticRunResult(
                success = false,
                logs = logs,
                errorMessage = "Native Library Load Failed: ${t.message}"
            )
        }

        logLine("==================================================")
        logLine("[DIAGNOSTICS]")

        try {
            val results = NativeCore.runDiagnosticsTest()
            if (results == null) {
                logError("[FAILURE] NativeCore.runDiagnosticsTest() returned null")
                return DiagnosticRunResult(
                    success = false,
                    logs = logs,
                    errorMessage = "runDiagnosticsTest returned null"
                )
            }

            for (res in results) {
                logLine(res)
            }

            logLine("==================================================")
            logLine("[DIAGNOSTICS COMPLETE]")
            return DiagnosticRunResult(success = true, logs = logs)

        } catch (t: Throwable) {
            logError("[FAILURE] Diagnostic Execution Threw Exception", t)
            return DiagnosticRunResult(
                success = false,
                logs = logs,
                errorMessage = "Diagnostic Execution Error: ${t.message}"
            )
        }
    }
}
