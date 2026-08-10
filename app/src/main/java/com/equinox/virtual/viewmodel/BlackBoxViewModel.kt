package com.equinox.virtual.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.equinox.virtual.manager.AllowedPackagesManager
import com.equinox.virtual.manager.AppSettingsManager
import com.equinox.virtual.manager.AuthAndUserManager
import com.equinox.virtual.manager.LicenseAndStatsManager
import com.equinox.virtual.manager.VirtualSpaceManager
import com.equinox.virtual.model.AllowedPackage
import com.equinox.virtual.model.FirestoreUser
import com.equinox.virtual.model.LicenseKey
import com.equinox.virtual.model.VirtualAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlackBoxViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = AppSettingsManager(application)
    private val virtualSpaceManager = VirtualSpaceManager(application)
    private val authUserManager = AuthAndUserManager(application, settingsManager.getPrefs())
    private val licenseStatsManager = LicenseAndStatsManager(settingsManager.getPrefs()) {
        authUserManager.getFirestoreDb()
    }
    private val allowedPackagesManager = AllowedPackagesManager {
        authUserManager.getFirestoreDb()
    }

    val allowedPackages: StateFlow<Set<String>> = allowedPackagesManager.allowedPackages
    val allowedPackageList: StateFlow<List<AllowedPackage>> = allowedPackagesManager.allowedPackageList

    val deviceSpoofingEnabled: StateFlow<Boolean> = settingsManager.deviceSpoofingEnabled
    val gmsProxyEnabled: StateFlow<Boolean> = settingsManager.gmsProxyEnabled
    val storageIsolationEnabled: StateFlow<Boolean> = settingsManager.storageIsolationEnabled
    val rootHideEnabled: StateFlow<Boolean> = settingsManager.rootHideEnabled
    val isDarkTheme: StateFlow<Boolean?> = settingsManager.isDarkTheme

    val currentUserId: StateFlow<Int> = virtualSpaceManager.currentUserId
    val virtualApps: StateFlow<List<VirtualAppInfo>> = virtualSpaceManager.virtualApps
    val hostApps: StateFlow<List<VirtualAppInfo>> = virtualSpaceManager.hostApps
    val userList: StateFlow<List<Int>> = virtualSpaceManager.userList
    val engineInitialized: StateFlow<Boolean> = virtualSpaceManager.engineInitialized
    val engineProgress: StateFlow<Float> = virtualSpaceManager.engineProgress
    val engineStatusText: StateFlow<String> = virtualSpaceManager.engineStatusText

    val currentUserSession: StateFlow<String?> = authUserManager.currentUserSession
    val isCheckingSession: StateFlow<Boolean> = authUserManager.isCheckingSession
    val expiryTime: StateFlow<Long?> = authUserManager.expiryTime
    val userRole: StateFlow<String?> = authUserManager.userRole
    val isRegisteredDevice: StateFlow<Boolean> = authUserManager.isRegisteredDevice
    val firestoreUsers: StateFlow<List<FirestoreUser>> = authUserManager.firestoreUsers
    val currentUserBalance: StateFlow<Long> = authUserManager.currentUserBalance

    val systemStats: StateFlow<Map<String, Int>> = licenseStatsManager.systemStats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        listenToAllowedPackages()
        checkEngineStatus()
        refreshAll()
        validateSession()
    }

    fun listenToAllowedPackages() {
        allowedPackagesManager.listenToAllowedPackages {
            viewModelScope.launch(Dispatchers.IO) {
                val pkgs = allowedPackages.value
                val pkgList = allowedPackageList.value
                virtualSpaceManager.loadVirtualApps(virtualSpaceManager.currentUserId.value, pkgs)
                virtualSpaceManager.loadHostApps(pkgs, pkgList)
            }
        }
    }

    fun addAllowedPackage(packageName: String, appName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUserUid = authUserManager.currentUserSession.value ?: "Admin"
            allowedPackagesManager.addAllowedPackage(
                packageName = packageName,
                appName = appName,
                addedBy = currentUserUid,
                onSuccess = {
                    _isLoading.value = false
                    viewModelScope.launch { _snackbarMessage.emit("Berhasil menambahkan APK clone: $packageName") }
                },
                onFailure = { errorMsg ->
                    _isLoading.value = false
                    viewModelScope.launch { _snackbarMessage.emit(errorMsg) }
                }
            )
        }
    }

    fun deleteAllowedPackage(packageName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            allowedPackagesManager.deleteAllowedPackage(
                packageName = packageName,
                onSuccess = {
                    _isLoading.value = false
                    viewModelScope.launch { _snackbarMessage.emit("Berhasil menghapus APK clone: $packageName") }
                },
                onFailure = { errorMsg ->
                    _isLoading.value = false
                    viewModelScope.launch { _snackbarMessage.emit(errorMsg) }
                }
            )
        }
    }

    fun toggleTheme(isCurrentlyDark: Boolean) {
        settingsManager.toggleTheme(isCurrentlyDark)
    }

    fun checkEngineStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            virtualSpaceManager.checkEngineStatus()
        }
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _snackbarMessage.emit("Sandbox sedang dimuat ulang...")
            
            // Re-check engine
            virtualSpaceManager.checkEngineStatus()
            
            val pkgs = allowedPackages.value
            val pkgList = allowedPackageList.value
            virtualSpaceManager.loadVirtualApps(virtualSpaceManager.currentUserId.value, pkgs)
            virtualSpaceManager.loadHostApps(pkgs, pkgList)
            virtualSpaceManager.loadUserList()
            
            _snackbarMessage.emit("Sandbox berhasil dimuat ulang!")
            _isLoading.value = false
        }
    }

    fun selectUser(userId: Int) {
        virtualSpaceManager.setCurrentUserId(userId)
        viewModelScope.launch(Dispatchers.IO) {
            virtualSpaceManager.loadVirtualApps(userId, allowedPackages.value)
        }
    }

    fun addUser() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newUserId = virtualSpaceManager.createVirtualUser()
                if (newUserId != null) {
                    _snackbarMessage.emit("Dibuat Pengguna Ruang Virtual $newUserId")
                    virtualSpaceManager.loadUserList()
                    selectUser(newUserId)
                } else {
                    _snackbarMessage.emit("Gagal membuat pengguna baru")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("Gagal membuat pengguna: ${e.message}")
            }
        }
    }

    fun deleteUser(userId: Int) {
        if (userId == 0) {
            viewModelScope.launch {
                _snackbarMessage.emit("Tidak dapat menghapus Pengguna Utama 0")
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                virtualSpaceManager.deleteVirtualUser(userId)
                _snackbarMessage.emit("Dihapus Pengguna Ruang Virtual $userId")
                virtualSpaceManager.loadUserList()
                selectUser(0)
            } catch (e: Exception) {
                _snackbarMessage.emit("Gagal menghapus pengguna: ${e.message}")
            }
        }
    }

    fun loadVirtualApps(userId: Int) {
        virtualSpaceManager.loadVirtualApps(userId)
    }

    fun installAppToVirtual(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = virtualSpaceManager.currentUserId.value
                val result = virtualSpaceManager.installPackageAsUser(packageName, userId)
                if (result.success) {
                    _snackbarMessage.emit("Aplikasi berhasil dikloning ke Pengguna $userId")
                    virtualSpaceManager.loadVirtualApps(userId)
                } else {
                    _snackbarMessage.emit("Pemasangan gagal: ${result.msg}")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("Kesalahan pemasangan: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uninstallVirtualApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = virtualSpaceManager.currentUserId.value
                virtualSpaceManager.uninstallPackageAsUser(packageName, userId)
                _snackbarMessage.emit("Berhasil mencopot $packageName dari Pengguna $userId")
                virtualSpaceManager.loadVirtualApps(userId)
            } catch (e: Exception) {
                _snackbarMessage.emit("Gagal mencopot pemasangan: ${e.message}")
            }
        }
    }

    fun launchVirtualApp(packageName: String, isModMode: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = virtualSpaceManager.currentUserId.value
                val success = virtualSpaceManager.launchApk(packageName, userId)
                if (success) {
                    _snackbarMessage.emit("Membuka $packageName (Mode Normal)...")
                } else {
                    _snackbarMessage.emit("Gagal membuka $packageName")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("Gagal membuka: ${e.message}")
            }
        }
    }

    fun clearVirtualAppData(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = virtualSpaceManager.currentUserId.value
                virtualSpaceManager.clearPackage(packageName, userId)
                _snackbarMessage.emit("Data $packageName berhasil dibersihkan")
            } catch (e: Exception) {
                _snackbarMessage.emit("Gagal membersihkan data: ${e.message}")
            }
        }
    }

    fun installApkFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = virtualSpaceManager.currentUserId.value
                _snackbarMessage.emit("Menginstal APK ke Bcore Virtual Space...")
                val (success, msg) = virtualSpaceManager.installFromUri(uri, userId)
                if (success) {
                    _snackbarMessage.emit("APK berhasil diinstal ke User Space $userId!")
                    virtualSpaceManager.loadVirtualApps(userId)
                } else {
                    _snackbarMessage.emit("Gagal menginstal APK: $msg")
                }
            } catch (e: Exception) {
                Log.e("BlackBoxViewModel", "Install APK error: ${e.message}", e)
                _snackbarMessage.emit("Error install APK: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadAndInstallApkFromUrl(urlStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _snackbarMessage.emit("Mengunduh APK dari URL...")
                val userId = virtualSpaceManager.currentUserId.value
                val (success, msg) = virtualSpaceManager.downloadAndInstallFromUrl(urlStr, userId)
                if (success) {
                    _snackbarMessage.emit("APK berhasil diinstal dari URL ke User Space $userId!")
                    virtualSpaceManager.loadVirtualApps(userId)
                } else {
                    _snackbarMessage.emit("Gagal menginstal APK: $msg")
                }
            } catch (e: Exception) {
                Log.e("BlackBoxViewModel", "Download APK error: ${e.message}", e)
                _snackbarMessage.emit("Error mengunduh APK: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setDeviceSpoofingEnabled(enabled: Boolean) {
        settingsManager.setDeviceSpoofingEnabled(enabled)
        viewModelScope.launch {
            _snackbarMessage.emit("Pemalsuan Perangkat: ${if (enabled) "Diaktifkan" else "Dinonaktifkan"}")
        }
    }

    fun setGmsProxyEnabled(enabled: Boolean) {
        settingsManager.setGmsProxyEnabled(enabled)
        viewModelScope.launch {
            _snackbarMessage.emit("Proksi Layanan Google Play: ${if (enabled) "Diaktifkan" else "Dinonaktifkan"}")
        }
    }

    fun setStorageIsolationEnabled(enabled: Boolean) {
        settingsManager.setStorageIsolationEnabled(enabled)
        viewModelScope.launch {
            _snackbarMessage.emit("Isolasi Penyimpanan: ${if (enabled) "Diaktifkan" else "Dinonaktifkan"}")
        }
    }

    fun setRootHideEnabled(enabled: Boolean) {
        settingsManager.setRootHideEnabled(enabled)
        viewModelScope.launch {
            _snackbarMessage.emit("Fitur Sembunyikan Root & Hook: ${if (enabled) "Diaktifkan" else "Dinonaktifkan"}")
        }
    }

    fun listenToCurrentUserSession() {
        authUserManager.listenToCurrentUserSession(
            onRoleChanged = { roleDisplay ->
                viewModelScope.launch {
                    _snackbarMessage.emit("Role Anda telah diperbarui menjadi $roleDisplay")
                }
            },
            onExpired = {
                viewModelScope.launch {
                    _snackbarMessage.emit("Masa aktif akun telah habis!")
                }
            }
        )
    }

    fun validateSession() {
        viewModelScope.launch {
            authUserManager.validateSession(
                onRoleChanged = { roleDisplay ->
                    viewModelScope.launch {
                        _snackbarMessage.emit("Role Anda telah diperbarui menjadi $roleDisplay")
                    }
                },
                onExpired = {
                    viewModelScope.launch {
                        _snackbarMessage.emit("Masa aktif akun telah habis!")
                    }
                }
            )
        }
    }

    fun authenticateDevice(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            android.widget.Toast.makeText(getApplication(), "Memulai autentikasi perangkat...", android.widget.Toast.LENGTH_SHORT).show()
            _isLoading.value = true
            authUserManager.authenticateDevice(
                onRoleChanged = { roleDisplay ->
                    viewModelScope.launch {
                        _snackbarMessage.emit("Role Anda telah diperbarui menjadi $roleDisplay")
                    }
                },
                onExpired = {
                    viewModelScope.launch {
                        _snackbarMessage.emit("Masa aktif akun telah habis!")
                    }
                },
                onResult = { success, msg ->
                    _isLoading.value = false
                    onResult(success, msg)
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            _snackbarMessage.emit("Sedang keluar...")
            authUserManager.logout()
            licenseStatsManager.clearListeners()
            _isLoading.value = false
            _snackbarMessage.emit("Berhasil keluar")
        }
    }

    fun fetchCurrentUserBalance() {
        authUserManager.fetchCurrentUserBalance()
    }

    fun fetchFirestoreUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            authUserManager.fetchFirestoreUsers { _, _ ->
                _isLoading.value = false
            }
        }
    }

    fun updateFirestoreUser(uid: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            authUserManager.updateFirestoreUser(
                uid = uid,
                updates = updates,
                onSuccess = {
                    _isLoading.value = false
                    fetchFirestoreUsers()
                    viewModelScope.launch { _snackbarMessage.emit("Berhasil memperbarui pengguna") }
                },
                onFailure = { errorMsg ->
                    _isLoading.value = false
                    viewModelScope.launch { _snackbarMessage.emit("Gagal: $errorMsg") }
                }
            )
        }
    }

    fun deleteFirestoreUser(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            authUserManager.deleteFirestoreUser(
                uid = uid,
                onSuccess = {
                    _isLoading.value = false
                    fetchFirestoreUsers()
                    viewModelScope.launch { _snackbarMessage.emit("Berhasil menghapus pengguna") }
                },
                onFailure = { errorMsg ->
                    _isLoading.value = false
                    viewModelScope.launch { _snackbarMessage.emit("Gagal: $errorMsg") }
                }
            )
        }
    }

    fun fetchSystemStats() {
        viewModelScope.launch {
            _isLoading.value = true
            licenseStatsManager.fetchSystemStats {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        authUserManager.clearListeners()
        licenseStatsManager.clearListeners()
        allowedPackagesManager.clearListener()
    }
}
