package com.equinox.virtual.model

data class AllowedPackage(
    val packageName: String = "",
    val appName: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val addedBy: String = ""
)
