package com.equinox.virtual

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import java.io.File

class EQuinoxApp : Application() {

    companion object {
        private const val TAG = "EQuinoxApp"
        private lateinit var instance: EQuinoxApp

        fun getContext(): Context = instance.applicationContext

        fun initFirebase(context: Context): FirebaseApp? {
            val appContext = context.applicationContext ?: context
            
            // 1. Check if already initialized
            try {
                val apps = FirebaseApp.getApps(appContext)
                if (apps.isNotEmpty()) {
                    return FirebaseApp.getInstance()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking for existing Firebase apps: ${e.message}")
            }

            // 2. Try standard initialization (using google-services.json values)
            try {
                val app = FirebaseApp.initializeApp(appContext)
                if (app != null) {
                    Log.d(TAG, "Standard Firebase initialization succeeded")
                    return app
                }
            } catch (e: Exception) {
                Log.w(TAG, "Standard Firebase initialization failed: ${e.message}")
            }
            
            // 3. Fallback to manual options as a LAST RESORT
            try {
                Log.d(TAG, "Attempting manual Firebase fallback...")
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:16158272696:android:96098a8fe11315125a984d")
                    .setApiKey("AIzaSyDm1ReFVcxRn_vU3NPt1_GLtJZ4kP1v7AE")
                    .setProjectId("equinox-28026")
                    .setStorageBucket("equinox-28026.firebasestorage.app")
                    .setGcmSenderId("16158272696")
                    .build()
                
                // Initialize as the default app explicitly if it's not already
                return FirebaseApp.initializeApp(appContext, options)
            } catch (ex: Exception) {
                Log.e(TAG, "ALL Firebase initialization attempts failed: ${ex.message}")
                return null
            }
        }

        fun getDeviceHwid(): String {
            return try {
                // 1. Try Widevine DRM Hardware Device Unique ID (Permanent hardware TEE identifier)
                val widevineId = getWidevineHardwareId()
                if (!widevineId.isNullOrEmpty()) {
                    return "EQ-$widevineId"
                }

                // 2. Fallback to Hardware Specs SHA-256 Fingerprint
                val context = getContext()
                val androidId = try {
                    android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    ) ?: ""
                } catch (e: Exception) {
                    ""
                }

                val hardwareSpecs = StringBuilder()
                    .append(android.os.Build.MANUFACTURER.uppercase()).append("|")
                    .append(android.os.Build.MODEL.uppercase()).append("|")
                    .append(android.os.Build.BOARD.uppercase()).append("|")
                    .append(android.os.Build.HARDWARE.uppercase()).append("|")
                    .append(android.os.Build.BRAND.uppercase()).append("|")
                    .append(android.os.Build.DEVICE.uppercase()).append("|")
                    .append(android.os.Build.PRODUCT.uppercase()).append("|")
                    .append(androidId)
                    .toString()

                val md = java.security.MessageDigest.getInstance("SHA-256")
                val digest = md.digest(hardwareSpecs.toByteArray(Charsets.UTF_8))
                val hexString = digest.fold("") { str, it -> str + "%02X".format(it) }.take(16)

                "EQ-$hexString"
            } catch (e: Exception) {
                Log.e(TAG, "Error generating HWID: ${e.message}")
                "EQ-ERROR"
            }
        }

        private fun getWidevineHardwareId(): String? {
            var mediaDrm: android.media.MediaDrm? = null
            return try {
                val widevineUuid = java.util.UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")
                mediaDrm = android.media.MediaDrm(widevineUuid)
                val deviceUniqueId = mediaDrm.getPropertyByteArray(android.media.MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
                if (deviceUniqueId != null && deviceUniqueId.isNotEmpty()) {
                    val md = java.security.MessageDigest.getInstance("SHA-256")
                    val digest = md.digest(deviceUniqueId)
                    digest.fold("") { str, it -> str + "%02X".format(it) }.take(16)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            } finally {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        mediaDrm?.close()
                    } else {
                        @Suppress("DEPRECATION")
                        mediaDrm?.release()
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
        // Ensure Firebase is initialized as early as possible
        try {
            initFirebase(this)
        } catch (e: Exception) {
            Log.e(TAG, "Early Firebase init failed: ${e.message}")
        }
        
        try {
            BlackBoxCore.get().closeCodeInit()
        } catch (e: Exception) {
            Log.e(TAG, "closeCodeInit error: ${e.message}")
        }
        try {
            BlackBoxCore.get().onBeforeMainApplicationAttach(this, base)
        } catch (e: Exception) {
            Log.e(TAG, "onBeforeMainApplicationAttach error: ${e.message}")
        }
        try {
            val prefs = base?.getSharedPreferences("equinox_virtual_prefs", Context.MODE_PRIVATE)
            val rootHide = prefs?.getBoolean("root_hide", true) ?: true

            BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
                override fun getHostPackageName(): String {
                    return packageName
                }

                override fun isHideRoot(): Boolean {
                    return rootHide
                }

                override fun isEnableDaemonService(): Boolean {
                    return true
                }

                override fun isUseVpnNetwork(): Boolean {
                    return false
                }

                override fun isDisableFlagSecure(): Boolean {
                    return false
                }

                override fun requestInstallPackage(file: File?, userId: Int): Boolean {
                    return false
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "doAttachBaseContext error: ${e.message}")
        }
        try {
            BlackBoxCore.get().onAfterMainApplicationAttach(this, base)
        } catch (e: Exception) {
            Log.e(TAG, "onAfterMainApplicationAttach error: ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        val processName = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.app.Application.getProcessName()
            } else {
                try {
                    val activityThreadClass = Class.forName("android.app.ActivityThread")
                    val method = activityThreadClass.getDeclaredMethod("currentProcessName")
                    method.isAccessible = true
                    method.invoke(null) as? String
                } catch (e: Exception) {
                    null
                } ?: try {
                    val file = java.io.File("/proc/self/cmdline")
                    val reader = java.io.BufferedReader(java.io.FileReader(file))
                    val name = reader.readLine()?.trim { it <= ' ' }
                    reader.close()
                    name
                } catch (e: Exception) {
                    null
                } ?: packageName
            }
        } catch (e: Exception) {
            packageName
        }

        val isMain = processName == packageName || !processName.contains(":")

        if (isMain) {
            // Initialize Firebase only in main process
            try {
                initFirebase(this)
            } catch (e: Exception) {
                Log.e(TAG, "FirebaseApp initialize error in main: ${e.message}")
            }

            try {
                com.equinox.virtual.core.VirtualSpoof.initSpoof()
            } catch (e: Throwable) {
                Log.w(TAG, "VirtualSpoof initSpoof warning: ${e.message}")
            }
        }
        try {
            BlackBoxCore.get().doCreate()
        } catch (e: Exception) {
            Log.e(TAG, "doCreate error: ${e.message}")
        }
    }
}
