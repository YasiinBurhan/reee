package com.equinox.virtual.model

data class LicenseKey(
    val key: String = "",
    val durationDays: Int = 0,
    val role: String = "member",
    val isUsed: Boolean = false,
    val usedBy: String? = null,
    val generatedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
