package com.equinox.virtual.model

import androidx.annotation.Keep

@Keep
data class FirestoreUser(
    val uid: String = "",
    val role: String = "member",
    val expiredAt: Long = 0,
    val status: String = "active",
    val balance: Long = 0,
    val createdAt: Long = 0
)
