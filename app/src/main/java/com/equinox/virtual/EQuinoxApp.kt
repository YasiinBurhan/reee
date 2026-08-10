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
                val context = getContext()
                val androidId = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                if (androidId.isNullOrEmpty()) {
                    "EQ-UNKNOWN"
                } else {
                    "EQ-${androidId.uppercase()}"
                }
            } catch (e: Exception) {
                "EQ-ERROR"
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
            val gmsProxy = prefs?.getBoolean("gms_proxy", true) ?: true

            BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
                override fun getHostPackageName(): String {
                    return packageName
                }

                override fun isHideRoot(): Boolean {
                    return rootHide
                }

                override fun isEnableDaemonService(): Boolean {
                    return false
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
