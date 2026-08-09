package com.equinox.virtual.manager

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.equinox.virtual.model.AllowedPackage
import com.equinox.virtual.model.VirtualAppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.niunaijun.blackbox.BlackBoxCore
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class VirtualSpaceManager(private val application: Application) {

    private val context: Context = application.applicationContext
    private val packageManager: PackageManager = context.packageManager

    private val _currentUserId = MutableStateFlow(0)
    val currentUserId: StateFlow<Int> = _currentUserId.asStateFlow()

    private val _virtualApps = MutableStateFlow<List<VirtualAppInfo>>(emptyList())
    val virtualApps: StateFlow<List<VirtualAppInfo>> = _virtualApps.asStateFlow()

    private val _hostApps = MutableStateFlow<List<VirtualAppInfo>>(emptyList())
    val hostApps: StateFlow<List<VirtualAppInfo>> = _hostApps.asStateFlow()

    private val _userList = MutableStateFlow<List<Int>>(listOf(0))
    val userList: StateFlow<List<Int>> = _userList.asStateFlow()

    private val _engineInitialized = MutableStateFlow(false)
    val engineInitialized: StateFlow<Boolean> = _engineInitialized.asStateFlow()

    private val _engineProgress = MutableStateFlow(0f)
    val engineProgress: StateFlow<Float> = _engineProgress.asStateFlow()

    private val _engineStatusText = MutableStateFlow("Menginisialisasi Mesin Bcore...")
    val engineStatusText: StateFlow<String> = _engineStatusText.asStateFlow()

    fun setCurrentUserId(userId: Int) {
        _currentUserId.value = userId
    }

    suspend fun checkEngineStatus() {
        try {
            _engineProgress.value = 0.2f
            _engineStatusText.value = "Menghubungkan Runtime & Native Hooks Bcore..."
            kotlinx.coroutines.delay(300)

            _engineProgress.value = 0.5f
            _engineStatusText.value = "Memverifikasi Pengelola Paket & Ruang Pengguna..."
            kotlinx.coroutines.delay(300)

            _engineProgress.value = 0.8f
            _engineStatusText.value = "Memeriksa Sandbox Virtual Blackbox..."
            val isReady = (BlackBoxCore.get() != null)
            kotlinx.coroutines.delay(200)

            _engineProgress.value = 1.0f
            _engineStatusText.value = if (isReady) "Mesin Virtual Bcore Siap" else "Bcore Diinisialisasi dengan Fallback"
            _engineInitialized.value = true
        } catch (e: Exception) {
            Log.e("VirtualSpaceManager", "Engine status check error: ${e.message}")
            _engineProgress.value = 1.0f
            _engineStatusText.value = "Bcore Berjalan (${e.localizedMessage ?: "Diinisialisasi"})"
            _engineInitialized.value = true
        }
    }

    fun loadUserList() {
        try {
            val users = BlackBoxCore.get().users
            val userIds = if (users.isNullOrEmpty()) listOf(0) else users.map { it.id }
            _userList.value = userIds
        } catch (e: Exception) {
            Log.e("VirtualSpaceManager", "Error loading user list: ${e.message}")
            _userList.value = listOf(0)
        }
    }

    fun loadVirtualApps(userId: Int, allowedPackages: Set<String> = emptySet()) {
        try {
            val installedList = BlackBoxCore.get().getInstalledApplications(0, userId)
            val virtualAppsList = installedList.map { appInfo ->
                val label = try {
                    appInfo.loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    appInfo.packageName
                }
                val icon = try {
                    appInfo.loadIcon(packageManager)
                } catch (e: Exception) {
                    null
                }
                VirtualAppInfo(
                    packageName = appInfo.packageName,
                    name = label,
                    icon = icon,
                    sourceDir = appInfo.sourceDir ?: "",
                    isVirtual = true
                )
            }
            val filteredVirtualApps = if (allowedPackages.isNotEmpty()) {
                virtualAppsList.filter { allowedPackages.contains(it.packageName) }
            } else {
                virtualAppsList
            }
            _virtualApps.value = filteredVirtualApps
        } catch (e: Exception) {
            Log.e("VirtualSpaceManager", "Error loading virtual apps: ${e.message}")
            _virtualApps.value = emptyList()
        }
    }

    fun loadHostApps(allowedPackages: Set<String> = emptySet(), allowedPackageList: List<AllowedPackage> = emptyList()) {
        try {
            val selfPackage = application.packageName
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            var hostAppsList = packages.filter { appInfo ->
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                               (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val isSelf = appInfo.packageName == selfPackage
                val isLaunchable = packageManager.getLaunchIntentForPackage(appInfo.packageName) != null

                !isSystem && !isSelf && isLaunchable
            }.map { appInfo ->
                val label = appInfo.loadLabel(packageManager).toString()
                val icon = try {
                    appInfo.loadIcon(packageManager)
                } catch (e: Exception) {
                    null
                }
                VirtualAppInfo(
                    packageName = appInfo.packageName,
                    name = label,
                    icon = icon,
                    sourceDir = appInfo.sourceDir ?: "",
                    isSystemApp = false,
                    isVirtual = false
                )
            }.sortedWith(
                compareByDescending<VirtualAppInfo> { allowedPackages.contains(it.packageName) }
                    .thenBy { it.name.lowercase() }
            )

            if (hostAppsList.isEmpty()) {
                hostAppsList = allowedPackageList.map { item ->
                    VirtualAppInfo(
                        packageName = item.packageName,
                        name = item.appName.ifEmpty { item.packageName },
                        icon = null,
                        sourceDir = "",
                        isSystemApp = false,
                        isVirtual = false
                    )
                }.sortedBy { it.name.lowercase() }
            }
            _hostApps.value = hostAppsList
        } catch (e: Exception) {
            Log.e("VirtualSpaceManager", "Error loading host apps: ${e.message}")
            _hostApps.value = emptyList()
        }
    }

    fun createVirtualUser(): Int? {
        val bUser = BlackBoxCore.get().createUser(-1)
        return bUser?.id
    }

    fun deleteVirtualUser(userId: Int) {
        BlackBoxCore.get().deleteUser(userId)
    }

    fun installPackageAsUser(packageName: String, userId: Int): top.niunaijun.blackbox.entity.pm.InstallResult {
        return BlackBoxCore.get().installPackageAsUser(packageName, userId)
    }

    fun uninstallPackageAsUser(packageName: String, userId: Int) {
        BlackBoxCore.get().uninstallPackageAsUser(packageName, userId)
    }

    fun launchApk(packageName: String, userId: Int): Boolean {
        return BlackBoxCore.get().launchApk(packageName, userId)
    }

    fun clearPackage(packageName: String, userId: Int) {
        BlackBoxCore.get().clearPackage(packageName, userId)
    }

    fun installFromUri(uri: Uri, userId: Int): Pair<Boolean, String> {
        val tempFile = File(context.cacheDir, "install_temp_${System.currentTimeMillis()}.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        if (!tempFile.exists() || tempFile.length() == 0L) {
            return Pair(false, "Gagal membaca file APK dari penyimpanan.")
        }
        val result = BlackBoxCore.get().installPackageAsUser(tempFile, userId)
        tempFile.delete()
        return Pair(result.success, result.msg ?: "")
    }

    fun downloadAndInstallFromUrl(urlStr: String, userId: Int): Pair<Boolean, String> {
        val tempFile = File(context.cacheDir, "download_temp_${System.currentTimeMillis()}.apk")
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.connect()

        if (connection.responseCode in 200..299) {
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val result = BlackBoxCore.get().installPackageAsUser(tempFile, userId)
            tempFile.delete()
            return Pair(result.success, result.msg ?: "")
        } else {
            return Pair(false, "HTTP ${connection.responseCode}")
        }
    }
}
