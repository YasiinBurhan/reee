package com.equinox.virtual.helper

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.security.MessageDigest
import java.util.Locale

object AntiTamper {
    private const val TAG = "AntiTamper"

    // Set your legitimate app's signature SHA-256 (uppercase hex).
    // If empty or if current signature is not in this list, the application will display a black screen.
    // Tip: Run the app once to see the SHA-256 signature hash of your app in the Logcat logs.
    private val ALLOWED_SIGNATURES = setOf(
        "YOUR_RELEASE_KEY_SHA256_HERE"
    )

    // Set to true to enforce signature verification and anti-tamper checking even in DEBUG/development builds.
    // Set to false to allow testing on your device using a debug key without being blocked by a black screen.
    private const val ENFORCE_ON_DEBUG = false

    /**
     * Checks if the application integrity has been compromised.
     * Returns true if tampering is detected (should trigger a black screen).
     */
    fun isAppTampered(context: Context): Boolean {
        val isDebugBuild = try {
            val clazz = Class.forName("${context.packageName}.BuildConfig")
            val field = clazz.getField("DEBUG")
            field.getBoolean(null)
        } catch (e: Exception) {
            false
        }

        if (isDebugBuild && !ENFORCE_ON_DEBUG) {
            Log.i(TAG, "Running in debug mode. Anti-tamper checks bypassed.")
            return false
        }

        // 1. Debugger Check
        if (isDebuggerAttached()) {
            Log.e(TAG, "Debugger detected!")
            return true
        }

        // 2. Hooking / Instrumentation Framework Check (Frida, Xposed, Substrate, libbypass etc.)
        if (isHookingFrameworkDetected()) {
            Log.e(TAG, "Hooking framework or injected library detected!")
            return true
        }

        // 3. Signature Verification - Disabled per user request
        // if (!verifyAppSignature(context)) {
        //     Log.e(TAG, "Signature mismatch detected! The app signature has been modified.")
        //     return true
        // }

        // 4. Dex / File Integrity Check
        if (isDexTampered(context)) {
            Log.e(TAG, "DEX or library integrity compromise detected!")
            return true
        }

        return false
    }

    fun getCurrentSignature(context: Context): String {
        return getAppSignatureSHA256(context)
    }

    private fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    private fun isHookingFrameworkDetected(): Boolean {
        try {
            // Check by looking for loaded classes of Xposed / Substrate / Frida
            val suspiciousClasses = arrayOf(
                "de.robv.android.xposed.XposedBridge",
                "de.robv.android.xposed.XposedHelper",
                "com.saurik.substrate.MS"
            )
            for (clazz in suspiciousClasses) {
                try {
                    Class.forName(clazz)
                    Log.w(TAG, "Found suspicious class indicating hooking environment: $clazz")
                    return true
                } catch (e: ClassNotFoundException) {
                    // Normal
                }
            }

            // Check loaded memory maps (/proc/self/maps) for frida, xposed, substrate, etc.
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists()) {
                BufferedReader(FileReader(mapsFile)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val lowerLine = line!!.lowercase(Locale.ROOT)
                        if (lowerLine.contains("shadowhook") || lowerLine.contains("bytehook")) {
                            continue
                        }
                        if (lowerLine.contains("frida") || 
                            lowerLine.contains("xposed") || 
                            lowerLine.contains("substrate") || 
                            lowerLine.contains("libbypass") ||
                            lowerLine.contains("hack") ||
                            lowerLine.contains("cheat") ||
                            lowerLine.contains("hook")) {
                            Log.w(TAG, "Suspicious maps entry found: $line")
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore / Log
        }
        return false
    }

    private fun verifyAppSignature(context: Context): Boolean {
        val currentSignature = getAppSignatureSHA256(context)
        Log.i(TAG, "--------------------------------------------------------")
        Log.i(TAG, "CURRENT APP SIGNATURE SHA-256: $currentSignature")
        Log.i(TAG, "To lock this app, copy the SHA-256 code above and add it to Firestore allowed_signatures")
        Log.i(TAG, "--------------------------------------------------------")

        // Retrieve cached signatures from SharedPreferences (which were synced from Firestore)
        val prefs = context.getSharedPreferences("equinox_virtual_prefs", Context.MODE_PRIVATE)
        val cachedSignatures = prefs.getStringSet("cached_allowed_signatures", emptySet()) ?: emptySet()

        // Combine hardcoded and Firestore-registered signatures
        val allAllowedSignatures = ALLOWED_SIGNATURES.map { it.uppercase().trim() }.toMutableSet().apply {
            addAll(cachedSignatures.map { it.uppercase().trim() })
        }

        // If no signatures are configured in both hardcoded AND Firestore yet, do not lock out the user
        val configuredSignatures = allAllowedSignatures.filter { it.isNotEmpty() && it != "YOUR_RELEASE_KEY_SHA256_HERE" }
        if (configuredSignatures.isEmpty()) {
            Log.w(TAG, "No allowed signatures configured yet in code or Firestore. Skipping verification to avoid lockout.")
            return true
        }

        val isDebugBuild = try {
            val clazz = Class.forName("${context.packageName}.BuildConfig")
            val field = clazz.getField("DEBUG")
            field.getBoolean(null)
        } catch (e: Exception) {
            false
        }

        if (isDebugBuild && !ENFORCE_ON_DEBUG) {
            Log.i(TAG, "Running in debug mode. Signature verification bypassed.")
            return true
        }

        return configuredSignatures.contains(currentSignature)
    }

    private fun getAppSignatureSHA256(context: Context): String {
        try {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            val sigs = info.signatures
            if (sigs != null && sigs.isNotEmpty()) {
                return getSHA256(sigs[0].toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving app signature: ${e.message}")
        }
        return ""
    }

    private fun getSHA256(bytes: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val sb = StringBuilder()
            for (b in digest) {
                sb.append(String.format("%02X", b))
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun isDexTampered(context: Context): Boolean {
        try {
            val apkPath = context.packageCodePath
            val apkFile = File(apkPath)
            if (!apkFile.exists() || apkFile.length() <= 0) {
                return true
            }
        } catch (e: Exception) {
            // Ignore
        }
        return false
    }
}
