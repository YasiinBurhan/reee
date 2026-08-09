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
            FirebaseApp.initializeApp(this)
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
