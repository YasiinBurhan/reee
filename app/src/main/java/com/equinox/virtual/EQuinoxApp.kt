package com.equinox.virtual

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
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
            try {
                if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                    return FirebaseApp.getInstance()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "getApps check warning: ${e.message}")
            }

            android.widget.Toast.makeText(appContext, "Initializing Firebase...", android.widget.Toast.LENGTH_SHORT).show()

            try {
                return FirebaseApp.getInstance()
            } catch (e: Throwable) {
                // Not initialized yet
            }

            try {
                return FirebaseApp.initializeApp(appContext)
            } catch (e: Throwable) {
                Log.w(TAG, "FirebaseApp auto initialize (appContext) failed (${e.message}), trying direct context...")
            }

            try {
                return FirebaseApp.initializeApp(context)
            } catch (e: Throwable) {
                Log.w(TAG, "FirebaseApp auto initialize (context) failed (${e.message}), attempting manual fallback...")
            }

            val options = try {
                com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:16158272696:android:96098a8fe11315125a984d")
                    .setApiKey("AIzaSyDm1ReFVcxRn_vU3NPt1_GLtJZ4kP1v7AE")
                    .setProjectId("equinox-28026")
                    .setStorageBucket("equinox-28026.firebasestorage.app")
                    .setGcmSenderId("16158272696")
                    .build()
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to build FirebaseOptions: ${e.message}")
                null
            }

            if (options != null) {
                android.widget.Toast.makeText(appContext, "Attempting manual Firebase config...", android.widget.Toast.LENGTH_SHORT).show()
                try {
                    return FirebaseApp.initializeApp(appContext, options)
                } catch (e: Throwable) {
                    if (e.message?.contains("already exists", ignoreCase = true) == true) {
                        try {
                            return FirebaseApp.getInstance()
                        } catch (ex: Throwable) {
                            // ignore
                        }
                    }
                    Log.w(TAG, "FirebaseApp manual initialize (appContext) error: ${e.message}")
                }

                try {
                    return FirebaseApp.initializeApp(context, options)
                } catch (e: Throwable) {
                    if (e.message?.contains("already exists", ignoreCase = true) == true) {
                        try {
                            return FirebaseApp.getInstance()
                        } catch (ex: Throwable) {
                            // ignore
                        }
                    }
                    Log.e(TAG, "FirebaseApp manual initialize (context) error: ${e.message}")
                }
            }

            return try {
                FirebaseApp.getInstance()
            } catch (e: Throwable) {
                Log.e(TAG, "FirebaseApp final fallback getInstance failed: ${e.message}")
                null
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
        val isMain = try {
            val currentProcess = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
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
                    val name = reader.readLine()?.trim()
                    reader.close()
                    name
                } catch (e: Exception) {
                    null
                } ?: packageName
            }
            !currentProcess.contains(":")
        } catch (e: Exception) {
            true
        }
        if (isMain) {
            try {
                initFirebase(this)
            } catch (e: Exception) {
                Log.e(TAG, "FirebaseApp initialize error: ${e.message}")
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
