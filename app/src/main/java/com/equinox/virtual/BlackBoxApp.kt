package com.equinox.virtual

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import java.io.File

class BlackBoxApp : Application() {

    companion object {
        private const val TAG = "BlackBoxApp"
        private lateinit var instance: BlackBoxApp

        fun getContext(): Context = instance.applicationContext

        fun initFirebase(context: Context): FirebaseApp? {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                return FirebaseApp.getInstance()
            }
            return try {
                FirebaseApp.initializeApp(context)
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseApp auto initialize failed (${e.message}), attempting manual fallback...")
                try {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:16158272696:android:96098a8fe11315125a984d")
                        .setApiKey("AIzaSyDm1ReFVcxRn_vU3NPt1_GLtJZ4kP1v7AE")
                        .setProjectId("equinox-28026")
                        .setStorageBucket("equinox-28026.firebasestorage.app")
                        .setGcmSenderId("16158272696")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                } catch (e2: Exception) {
                    Log.e(TAG, "FirebaseApp manual initialize error: ${e2.message}")
                    null
                }
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
            BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
                override fun getHostPackageName(): String {
                    return packageName
                }

                override fun isHideRoot(): Boolean {
                    return false
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
        try {
            BlackBoxCore.get().doCreate()
        } catch (e: Exception) {
            Log.e(TAG, "doCreate error: ${e.message}")
        }
    }
}
