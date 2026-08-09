package com.equinox.virtual.core

import android.util.Log

object VirtualSpoof {
    private const val TAG = "VirtualSpoof"

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
    external fun initSpoof()
}
