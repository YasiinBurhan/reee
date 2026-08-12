package com.equinox.virtual.core

import android.os.Process
import android.util.Log
import androidx.annotation.Keep
import dalvik.system.DexFile
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.core.IOCore
import top.niunaijun.blackbox.core.system.JarManager
import top.niunaijun.blackbox.utils.Slog
import top.niunaijun.blackbox.utils.compat.DexFileCompat
import java.io.File

object NativeCore {
    const val TAG = "NativeCore"

    init {
        try {
            System.loadLibrary("bytehook")
        } catch (e: Throwable) {
            Log.w(TAG, "bytehook load warning: ${e.message}")
        }
        try {
            System.loadLibrary("blackbox")
        } catch (e: Throwable) {
            Log.e(TAG, "blackbox load error: ${e.message}")
        }
    }

    @JvmStatic
    external fun init(apiLevel: Int)

    @JvmStatic
    external fun enableIO()

    @JvmStatic
    external fun addIORule(targetPath: String, relocatePath: String)

    @JvmStatic
    external fun hideXposed()

    @JvmStatic
    external fun disableHiddenApi(): Boolean

    @JvmStatic
    external fun disableResourceLoading(): Boolean

    @JvmStatic
    external fun initMenuModSurfaceHook(packageName: String)

    @JvmStatic
    external fun setMenuModHookEnabled(enabled: Boolean)

    @JvmStatic
    external fun runDiagnosticsTest(): Array<String>?

    @Keep
    @JvmStatic
    fun getCallingUid(origCallingUid: Int): Int {
        return try {
            // When running in host process (main process), do not modify the calling UID
            if (!BlackBoxCore.get().isBlackProcess) {
                return origCallingUid
            }

            if (origCallingUid in 1 until Process.FIRST_APPLICATION_UID) return origCallingUid
            if (origCallingUid > Process.LAST_APPLICATION_UID) return origCallingUid

            val callingBUid = BlackBoxCore.getCallingBUid()
            if (callingBUid in 1 until Process.LAST_APPLICATION_UID) {
                return callingBUid
            }

            origCallingUid
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCallingUid: " + e.message)
            origCallingUid
        }
    }

    @Keep
    @JvmStatic
    fun redirectPath(path: String): String {
        return IOCore.get().redirectPath(path)
    }

    @Keep
    @JvmStatic
    fun redirectPath(path: File): File {
        return IOCore.get().redirectPath(path)
    }

    @Keep
    @JvmStatic
    @Suppress("DEPRECATION")
    fun loadEmptyDex(): LongArray {
        try {
            var emptyJar = JarManager.getInstance().emptyJar
            if (emptyJar == null) {
                Log.w(TAG, "Empty JAR not available, attempting sync initialization")
                JarManager.getInstance().initializeSync()
                emptyJar = JarManager.getInstance().emptyJar
            }

            if (emptyJar == null || !emptyJar.exists()) {
                Log.e(TAG, "Empty JAR file not found or invalid")
                return longArrayOf()
            }

            val dexFile = DexFile(emptyJar)
            val cookies: List<Long> = DexFileCompat.getCookies(dexFile)
            val longs = LongArray(cookies.size)
            for (i in cookies.indices) {
                longs[i] = cookies[i]
            }
            Log.d(TAG, "Successfully loaded empty DEX with " + cookies.size + " cookies")
            return longs
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load empty DEX", e)
        }
        return longArrayOf()
    }

    @Suppress("DEPRECATION")
    private fun createFallbackEmptyDex(): LongArray {
        try {
            Slog.d(TAG, "Creating fallback empty DEX")
            val emptyDexBytes = createMinimalDexBytes()
            val tempDexFile = File.createTempFile("fallback_empty", ".dex")
            tempDexFile.deleteOnExit()

            val fos = java.io.FileOutputStream(tempDexFile)
            fos.write(emptyDexBytes)
            fos.close()

            val dexFile = DexFile(tempDexFile)
            val cookies: List<Long> = DexFileCompat.getCookies(dexFile)

            if (cookies.isNotEmpty()) {
                val longs = LongArray(cookies.size)
                for (i in cookies.indices) {
                    longs[i] = cookies[i]
                }
                Slog.d(TAG, "Successfully created fallback empty DEX with " + cookies.size + " cookies")
                return longs
            }
        } catch (e: Exception) {
            Slog.e(TAG, "Error creating fallback empty DEX: " + e.message)
        }

        Slog.w(TAG, "Returning empty DEX array as last resort")
        return longArrayOf()
    }

    private fun createMinimalDexBytes(): ByteArray {
        return byteArrayOf(
            'd'.code.toByte(), 'e'.code.toByte(), 'x'.code.toByte(), '\n'.code.toByte(),
            0x30, 0x33, 0x35, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x70, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        )
    }
}
