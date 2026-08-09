package com.equinox.virtual.model

import android.graphics.drawable.Drawable

data class VirtualAppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    val sourceDir: String,
    val isSystemApp: Boolean = false,
    val isVirtual: Boolean = true
)
