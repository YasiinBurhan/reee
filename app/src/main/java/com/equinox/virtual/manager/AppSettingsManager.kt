package com.equinox.virtual.manager

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("equinox_virtual_prefs", Context.MODE_PRIVATE)

    private val _deviceSpoofingEnabled = MutableStateFlow(prefs.getBoolean("device_spoofing", true))
    val deviceSpoofingEnabled: StateFlow<Boolean> = _deviceSpoofingEnabled.asStateFlow()

    private val _gmsProxyEnabled = MutableStateFlow(prefs.getBoolean("gms_proxy", true))
    val gmsProxyEnabled: StateFlow<Boolean> = _gmsProxyEnabled.asStateFlow()

    private val _storageIsolationEnabled = MutableStateFlow(prefs.getBoolean("storage_isolation", true))
    val storageIsolationEnabled: StateFlow<Boolean> = _storageIsolationEnabled.asStateFlow()

    private val _rootHideEnabled = MutableStateFlow(prefs.getBoolean("root_hide", true))
    val rootHideEnabled: StateFlow<Boolean> = _rootHideEnabled.asStateFlow()

    private val _menuModSurfaceEnabled = MutableStateFlow(prefs.getBoolean("menumod_surface_enabled", true))
    val menuModSurfaceEnabled: StateFlow<Boolean> = _menuModSurfaceEnabled.asStateFlow()

    init {
        if (_menuModSurfaceEnabled.value) {
            try {
                com.equinox.virtual.core.NativeCore.initMenuModSurfaceHook("VirtualContainer.Admin")
                com.equinox.virtual.core.NativeCore.setMenuModHookEnabled(true)
            } catch (_: Throwable) {}
        }
    }

    private val _isDarkTheme = MutableStateFlow<Boolean?>(
        if (prefs.contains("is_dark_theme")) prefs.getBoolean("is_dark_theme", false) else null
    )
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    fun toggleTheme(isCurrentlyDark: Boolean) {
        val nextValue = !isCurrentlyDark
        _isDarkTheme.value = nextValue
        prefs.edit().putBoolean("is_dark_theme", nextValue).apply()
    }

    fun setMenuModSurfaceEnabled(enabled: Boolean) {
        _menuModSurfaceEnabled.value = enabled
        prefs.edit().putBoolean("menumod_surface_enabled", enabled).apply()
        try {
            if (enabled) {
                com.equinox.virtual.core.NativeCore.initMenuModSurfaceHook("VirtualContainer.Admin")
                com.equinox.virtual.core.NativeCore.setMenuModHookEnabled(true)
            } else {
                com.equinox.virtual.core.NativeCore.setMenuModHookEnabled(false)
            }
        } catch (_: Throwable) {}
    }

    fun setDeviceSpoofingEnabled(enabled: Boolean) {
        _deviceSpoofingEnabled.value = enabled
        prefs.edit().putBoolean("device_spoofing", enabled).apply()
    }

    fun setGmsProxyEnabled(enabled: Boolean) {
        _gmsProxyEnabled.value = enabled
        prefs.edit().putBoolean("gms_proxy", enabled).apply()
    }

    fun setStorageIsolationEnabled(enabled: Boolean) {
        _storageIsolationEnabled.value = enabled
        prefs.edit().putBoolean("storage_isolation", enabled).apply()
    }

    fun setRootHideEnabled(enabled: Boolean) {
        _rootHideEnabled.value = enabled
        prefs.edit().putBoolean("root_hide", enabled).apply()
    }

    fun getPrefs(): SharedPreferences = prefs
}
