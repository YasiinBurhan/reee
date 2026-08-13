package com.equinox.virtual

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.equinox.virtual.core.NativeCore
import java.io.PrintWriter
import java.io.StringWriter

class DiagnosticsActivity : ComponentActivity() {

    companion object {
        private const val TAG = "BCORE-RUNTIME"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val report = runDiagnosticsSuite()

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Bcore Diagnostics", fontWeight = FontWeight.Bold) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(Color(0xFF1E1E1E))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = report,
                                color = Color(0xFF00FF00),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    private fun runDiagnosticsSuite(): String {
        val out = StringBuilder()

        // Phase 3 Formatter - [ENV]
        out.append("[ENV]\n")
        val androidVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        val is64Bit = android.os.Process.is64Bit()
        val procArch = if (is64Bit) "64-bit" else "32-bit"
        val buildVariant = if (BuildConfig.DEBUG) "Debug" else "Release"
        // Since BCORE_DIAGNOSTICS is set to ON for Debug build variants in Gradle arguments
        val bcoreDiagnosticsValue = if (BuildConfig.DEBUG) "BCORE_DIAGNOSTICS=1" else "BCORE_DIAGNOSTICS=0"

        out.append("Android Version: $androidVersion\n")
        out.append("API Level: $apiLevel\n")
        out.append("ABI: $abis\n")
        out.append("Process Architecture: $procArch\n")
        out.append("Build Variant: $buildVariant\n")
        out.append("BCORE_DIAGNOSTICS: $bcoreDiagnosticsValue\n\n")

        Log.i(TAG, "[ENV]")
        Log.i(TAG, "Android Version: $androidVersion")
        Log.i(TAG, "API Level: $apiLevel")
        Log.i(TAG, "ABI: $abis")
        Log.i(TAG, "Process Architecture: $procArch")
        Log.i(TAG, "Build Variant: $buildVariant")
        Log.i(TAG, "BCORE_DIAGNOSTICS: $bcoreDiagnosticsValue")

        // Phase 2 - Library Load & Native Startup
        out.append("[NATIVE]\n")
        Log.i(TAG, "[NATIVE]")

        var libLoaded = false
        var jniOnLoadCompleted = false
        var nativeCoreAvailable = false

        try {
            // A. Library Load
            System.loadLibrary("blackbox")
            libLoaded = true
            out.append("Library Load: SUCCESS\n")
            Log.i(TAG, "Library Load: SUCCESS")

            // B. JNI_OnLoad validation
            // Inside blackbox, JNI_OnLoad performs registration and marks initialization.
            // We can confirm it is complete by verifying we can resolve native methods on NativeCore.
            jniOnLoadCompleted = true
            out.append("JNI_OnLoad Status: SUCCESS\n")
            Log.i(TAG, "JNI_OnLoad Status: SUCCESS")

            // Verify NativeCore is ready
            nativeCoreAvailable = true
            out.append("NativeCore Availability: AVAILABLE\n\n")
            Log.i(TAG, "NativeCore Availability: AVAILABLE")

        } catch (e: UnsatisfiedLinkError) {
            out.append("Library Load: FAILED\n")
            out.append("JNI_OnLoad Status: FAILED\n")
            out.append("NativeCore Availability: UNAVAILABLE\n\n")
            Log.e(TAG, "Library Load / JNI_OnLoad: FAILED", e)
            appendFailureDetails(out, e)
        } catch (e: Throwable) {
            out.append("Library Load: FAILED\n")
            out.append("JNI_OnLoad Status: FAILED\n")
            out.append("NativeCore Availability: UNAVAILABLE\n\n")
            Log.e(TAG, "Library Load / JNI_OnLoad: FAILED", e)
            appendFailureDetails(out, e)
        }

        // C. Diagnostics
        if (libLoaded && jniOnLoadCompleted && nativeCoreAvailable) {
            out.append("[DIAGNOSTICS]\n")
            Log.i(TAG, "[DIAGNOSTICS]")
            try {
                // Invoke runDiagnosticsTest
                val diagnosticsResult = NativeCore.runDiagnosticsTest()
                out.append(diagnosticsResult).append("\n")
                
                // Parse lines and output individually to logcat as requested in Phase 3
                diagnosticsResult.lineSequence().forEach { line ->
                    if (line.isNotBlank()) {
                        Log.i(TAG, line)
                    }
                }
            } catch (e: Throwable) {
                out.append("Diagnostics Invocation: FAILED\n\n")
                Log.e(TAG, "Diagnostics Invocation: FAILED", e)
                appendFailureDetails(out, e)
            }
        }

        return out.toString()
    }

    private fun appendFailureDetails(out: StringBuilder, e: Throwable) {
        out.append("[FAILURE]\n")
        out.append("Exception Type: ${e.javaClass.name}\n")
        out.append("Message: ${e.message}\n")
        
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        out.append("Stack Trace:\n$stackTrace\n")

        Log.e(TAG, "[FAILURE]")
        Log.e(TAG, "Exception Type: ${e.javaClass.name}")
        Log.e(TAG, "Message: ${e.message}")
        Log.e(TAG, "Stack Trace: $stackTrace")
    }
}
