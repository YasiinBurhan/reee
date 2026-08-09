package com.equinox.virtual.model

import androidx.annotation.Keep

@Keep
data class AllowedPackage(
    val packageName: String = "",
    val appName: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val addedBy: String = ""
)
