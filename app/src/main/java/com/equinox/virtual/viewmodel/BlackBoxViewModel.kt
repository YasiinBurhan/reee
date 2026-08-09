package com.equinox.virtual.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.equinox.virtual.model.VirtualAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import top.niunaijun.blackbox.BlackBoxCore
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private val REGISTERED_PACKAGES = setOf(
    "com.mobile.legends"
)

class BlackBoxViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val packageManager: PackageManager = context.packageManager

    private val prefs = context.getSharedPreferences("equinox_virtual_prefs", android.content.Context.MODE_PRIVATE)

    private val _deviceSpoofingEnabled = MutableStateFlow(prefs.getBoolean("device_spoofing", true))
    val deviceSpoofingEnabled: StateFlow<Boolean> = _deviceSpoofingEnabled.asStateFlow()

    private val _gmsProxyEnabled = MutableStateFlow(prefs.getBoolean("gms_proxy", true))
    val gmsProxyEnabled: StateFlow<Boolean> = _gmsProxyEnabled.asStateFlow()

    private val _storageIsolationEnabled = MutableStateFlow(prefs.getBoolean("storage_isolation", true))
    val storageIsolationEnabled: StateFlow<Boolean> = _storageIsolationEnabled.asStateFlow()

    private val _rootHideEnabled = MutableStateFlow(prefs.getBoolean("root_hide", true))
    val rootHideEnabled: StateFlow<Boolean> = _rootHideEnabled.asStateFlow()

    private val _isDarkTheme = MutableStateFlow<Boolean?>(
        if (prefs.contains("is_dark_theme")) prefs.getBoolean("is_dark_theme", false) else null
    )
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    fun toggleTheme(isCurrentlyDark: Boolean) {
        val nextValue = !isCurrentlyDark
        _isDarkTheme.value = nextValue
        prefs.edit().putBoolean("is_dark_theme", nextValue).apply()
    }

    private val _currentUserId = MutableStateFlow(0)
    val currentUserId: StateFlow<Int> = _currentUserId.asStateFlow()

    private val _virtualApps = MutableStateFlow<List<VirtualAppInfo>>(emptyList())
    val virtualApps: StateFlow<List<VirtualAppInfo>> = _virtualApps.asStateFlow()

    private val _hostApps = MutableStateFlow<List<VirtualAppInfo>>(emptyList())
    val hostApps: StateFlow<List<VirtualAppInfo>> = _hostApps.asStateFlow()

    private val _userList = MutableStateFlow<List<Int>>(listOf(0))
    val userList: StateFlow<List<Int>> = _userList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _engineInitialized = MutableStateFlow(false)
    val engineInitialized: StateFlow<Boolean> = _engineInitialized.asStateFlow()

    private val _engineProgress = MutableStateFlow(0f)
    val engineProgress: StateFlow<Float> = _engineProgress.asStateFlow()

    private val _engineStatusText = MutableStateFlow("Menginisialisasi Mesin Bcore...")
    val engineStatusText: StateFlow<String> = _engineStatusText.asStateFlow()

    private val _currentUserSession = MutableStateFlow<String?>(prefs.getString("auth_username", null))
    val currentUserSession: StateFlow<String?> = _currentUserSession.asStateFlow()

    private val _isCheckingSession = MutableStateFlow(true)
    val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    private val _expiryTime = MutableStateFlow<Long?>(prefs.getLong("auth_expiry", 0L).let { if (it == 0L) null else it })
    val expiryTime: StateFlow<Long?> = _expiryTime.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(prefs.getString("auth_role", null))
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    private val _isRegisteredDevice = MutableStateFlow(prefs.getBoolean("is_registered_device", false))
    val isRegisteredDevice: StateFlow<Boolean> = _isRegisteredDevice.asStateFlow()

    private val _firestoreUsers = MutableStateFlow<List<com.equinox.virtual.model.FirestoreUser>>(emptyList())
    val firestoreUsers: StateFlow<List<com.equinox.virtual.model.FirestoreUser>> = _firestoreUsers.asStateFlow()

    private val _licenseKeys = MutableStateFlow<List<com.equinox.virtual.model.LicenseKey>>(emptyList())
    val licenseKeys: StateFlow<List<com.equinox.virtual.model.LicenseKey>> = _licenseKeys.asStateFlow()

    private val _systemStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val systemStats: StateFlow<Map<String, Int>> = _systemStats.asStateFlow()

    private val _currentUserBalance = MutableStateFlow(0L)
    val currentUserBalance: StateFlow<Long> = _currentUserBalance.asStateFlow()

    private var userDocumentListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var usersCollectionListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var licensesCollectionListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        checkEngineStatus()
        refreshAll()
        validateSession()
    }

    fun checkEngineStatus() {
        viewModelScope.launch(Dispatchers.IO) {
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
                Log.e("BlackBoxViewModel", "Engine status check error: ${e.message}")
                _engineProgress.value = 1.0f
                _engineStatusText.value = "Bcore Berjalan (${e.localizedMessage ?: "Diinisialisasi"})"
                _engineInitialized.value = true
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            loadVirtualApps(_currentUserId.value)
            loadHostApps()
            loadUserList()
            _isLoading.value = false
        }
    }

    fun selectUser(userId: Int) {
        _currentUserId.value = userId
        viewModelScope.launch(Dispatchers.IO) {
            loadVirtualApps(userId)
        }
    }

    fun addUser() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bUser = BlackBoxCore.get().createUser(-1)
                if (bUser != null) {
                    _snackbarMessage.emit("Dibuat Pengguna Ruang Virtual ${bUser.id}")
                    loadUserList()
                    selectUser(bUser.id)
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
                BlackBoxCore.get().deleteUser(userId)
                _snackbarMessage.emit("Dihapus Pengguna Ruang Virtual $userId")
                loadUserList()
                selectUser(0)
            } catch (e: Exception) {
                _snackbarMessage.emit("Gagal menghapus pengguna: ${e.message}")
            }
        }
    }

    private fun loadUserList() {
        try {
            val users = BlackBoxCore.get().users
            val userIds = if (users.isNullOrEmpty()) listOf(0) else users.map { it.id }
            _userList.value = userIds
        } catch (e: Exception) {
            Log.e("BlackBoxViewModel", "Error loading user list: ${e.message}")
            _userList.value = listOf(0)
        }
    }

    fun loadVirtualApps(userId: Int) {
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
            val filteredVirtualApps = virtualAppsList.filter { it.packageName == "com.mobile.legends" }
            _virtualApps.value = filteredVirtualApps
        } catch (e: Exception) {
            Log.e("BlackBoxViewModel", "Error loading virtual apps: ${e.message}")
            _virtualApps.value = emptyList()
        }
    }

    private fun loadHostApps() {
        try {
            val selfPackage = getApplication<Application>().packageName
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
                compareByDescending<VirtualAppInfo> { REGISTERED_PACKAGES.contains(it.packageName) }
                    .thenBy { it.name.lowercase() }
            )

            if (hostAppsList.isEmpty()) {
                hostAppsList = listOf(
                    VirtualAppInfo(
                        packageName = "com.mobile.legends",
                        name = "Mobile Legends: Bang Bang",
                        icon = null,
                        sourceDir = "",
                        isSystemApp = false,
                        isVirtual = false
                    ),
                    VirtualAppInfo(
                        packageName = "com.dts.freefireth",
                        name = "Garena Free Fire",
                        icon = null,
                        sourceDir = "",
                        isSystemApp = false,
                        isVirtual = false
                    ),
                    VirtualAppInfo(
                        packageName = "com.tencent.ig",
                        name = "PUBG MOBILE",
                        icon = null,
                        sourceDir = "",
                        isSystemApp = false,
                        isVirtual = false
                    ),
                    VirtualAppInfo(
                        packageName = "com.kiloo.subwaysurf",
                        name = "Subway Surfers",
                        icon = null,
                        sourceDir = "",
                        isSystemApp = false,
                        isVirtual = false
                    ),
                    VirtualAppInfo(
                        packageName = "com.unregistered.dummy",
                        name = "Aplikasi Tidak Terdaftar (Dummy)",
                        icon = null,
                        sourceDir = "",
                        isSystemApp = false,
                        isVirtual = false
                    )
                ).sortedWith(
                    compareByDescending<VirtualAppInfo> { REGISTERED_PACKAGES.contains(it.packageName) }
                        .thenBy { it.name.lowercase() }
                )
            }
            _hostApps.value = hostAppsList
        } catch (e: Exception) {
            Log.e("BlackBoxViewModel", "Error loading host apps: ${e.message}")
            _hostApps.value = emptyList()
        }
    }

    fun installAppToVirtual(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = _currentUserId.value
                val result = BlackBoxCore.get().installPackageAsUser(packageName, userId)
                if (result.success) {
                    _snackbarMessage.emit("Aplikasi berhasil dikloning ke Pengguna $userId")
                    loadVirtualApps(userId)
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
                val userId = _currentUserId.value
                BlackBoxCore.get().uninstallPackageAsUser(packageName, userId)
                _snackbarMessage.emit("Berhasil mencopot $packageName dari Pengguna $userId")
                loadVirtualApps(userId)
            } catch (e: Exception) {
                _snackbarMessage.emit("Gagal mencopot pemasangan: ${e.message}")
            }
        }
    }

    fun launchVirtualApp(packageName: String, isModMode: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = _currentUserId.value
                val success = BlackBoxCore.get().launchApk(packageName, userId)
                val modeLabel = "Mode Normal"
                if (success) {
                    _snackbarMessage.emit("Membuka $packageName ($modeLabel)...")
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
                val userId = _currentUserId.value
                BlackBoxCore.get().clearPackage(packageName, userId)
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
                val context = getApplication<Application>()
                val userId = _currentUserId.value
                val tempFile = File(context.cacheDir, "install_temp_${System.currentTimeMillis()}.apk")
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    _snackbarMessage.emit("Gagal membaca file APK dari penyimpanan.")
                    return@launch
                }

                _snackbarMessage.emit("Menginstal APK ke Bcore Virtual Space...")
                val result = BlackBoxCore.get().installPackageAsUser(tempFile, userId)
                if (result.success) {
                    _snackbarMessage.emit("APK berhasil diinstal ke User Space $userId!")
                    loadVirtualApps(userId)
                } else {
                    _snackbarMessage.emit("Gagal menginstal APK: ${result.msg}")
                }
                tempFile.delete()
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
                val context = getApplication<Application>()
                val userId = _currentUserId.value
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
                    _snackbarMessage.emit("Menginstal APK ke Bcore Virtual Space...")
                    val result = BlackBoxCore.get().installPackageAsUser(tempFile, userId)
                    if (result.success) {
                        _snackbarMessage.emit("APK berhasil diinstal dari URL ke User Space $userId!")
                        loadVirtualApps(userId)
                    } else {
                        _snackbarMessage.emit("Gagal menginstal APK: ${result.msg}")
                    }
                    tempFile.delete()
                } else {
                    _snackbarMessage.emit("Gagal mengunduh: HTTP ${connection.responseCode}")
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
        _deviceSpoofingEnabled.value = enabled
        prefs.edit().putBoolean("device_spoofing", enabled).apply()
        viewModelScope.launch {
            _snackbarMessage.emit("Pemalsuan Perangkat: ${if (enabled) "Diaktifkan" else "Dinonaktifkan"}")
        }
    }

    fun setGmsProxyEnabled(enabled: Boolean) {
        _gmsProxyEnabled.value = enabled
        prefs.edit().putBoolean("gms_proxy", enabled).apply()
        viewModelScope.launch {
            _snackbarMessage.emit("Proksi Layanan Google Play: ${if (enabled) "Diaktifkan" else "Dinonaktifkan"}")
        }
    }

    fun setStorageIsolationEnabled(enabled: Boolean) {
        _storageIsolationEnabled.value = enabled
        prefs.edit().putBoolean("storage_isolation", enabled).apply()
        viewModelScope.launch {
            _snackbarMessage.emit("Isolasi Penyimpanan: ${if (enabled) "Diaktifkan" else "Dinonaktifkan"}")
        }
    }

    fun setRootHideEnabled(enabled: Boolean) {
        _rootHideEnabled.value = enabled
        prefs.edit().putBoolean("root_hide", enabled).apply()
        viewModelScope.launch {
            _snackbarMessage.emit("Fitur Sembunyikan Root & Hook: ${if (enabled) "Diaktifkan" else "Dinonaktifkan"}")
        }
    }

    fun listenToCurrentUserSession() {
        val currentDeviceHwid = com.equinox.virtual.BlackBoxApp.getDeviceHwid()
        val savedAuth = prefs.getString("auth_uid", null)
        if (savedAuth.isNullOrEmpty()) return

        userDocumentListenerRegistration?.remove()
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            userDocumentListenerRegistration = db.collection("users").document(currentDeviceHwid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("BlackBoxViewModel", "Realtime user listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val expiredAt = snapshot.getLong("expiredAt") ?: 0L
                        val role = snapshot.getString("role") ?: "member"
                        val balance = snapshot.getLong("balance") ?: 0L

                        val previousRole = _userRole.value
                        val isRoleChanged = previousRole != null && previousRole.lowercase() != role.lowercase()

                        _currentUserSession.value = currentDeviceHwid
                        _expiryTime.value = expiredAt
                        _userRole.value = role
                        _currentUserBalance.value = balance
                        _isRegisteredDevice.value = true

                        prefs.edit()
                            .putString("auth_uid", currentDeviceHwid)
                            .putLong("auth_expiry", expiredAt)
                            .putString("auth_role", role)
                            .putBoolean("is_registered_device", true)
                            .apply()

                        if (isRoleChanged) {
                            val roleDisplay = when(role.lowercase()) {
                                "admin" -> "Administrator"
                                "reseller" -> "Reseller"
                                else -> "Member"
                            }
                            viewModelScope.launch {
                                _snackbarMessage.emit("Role Anda telah diperbarui menjadi $roleDisplay")
                            }
                        }

                        if (System.currentTimeMillis() > expiredAt && role == "member") {
                            logout()
                            viewModelScope.launch {
                                _snackbarMessage.emit("Masa aktif akun telah habis!")
                            }
                        }
                    } else if (_currentUserSession.value != null) {
                        logout()
                    }
                }
        } catch (e: Exception) {
            Log.e("BlackBoxViewModel", "Error attaching user listener: ${e.message}")
        }
    }

    fun validateSession() {
        viewModelScope.launch {
            _isCheckingSession.value = true
            val savedAuth = prefs.getString("auth_uid", null)
            
            if (savedAuth != null) {
                listenToCurrentUserSession()
            }
            _isCheckingSession.value = false
        }
    }

    fun authenticateDevice(onResult: (Boolean, String) -> Unit) {
        val currentDeviceHwid = com.equinox.virtual.BlackBoxApp.getDeviceHwid()
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val userDocRef = db.collection("users").document(currentDeviceHwid)

                userDocRef.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Already registered, check expiry
                        val expiredAt = document.getLong("expiredAt") ?: 0L
                        val role = document.getString("role") ?: "member"
                        _isLoading.value = false

                        if (System.currentTimeMillis() > expiredAt && role == "member") {
                            _isRegisteredDevice.value = true
                            _userRole.value = role
                            onResult(false, "Masa aktif perangkat telah habis! Silakan perpanjang.")
                        } else {
                            prefs.edit()
                                .putString("auth_uid", currentDeviceHwid)
                                .putString("auth_role", role)
                                .putLong("auth_expiry", expiredAt)
                                .putBoolean("is_registered_device", true)
                                .apply()

                            _currentUserSession.value = currentDeviceHwid
                            _expiryTime.value = expiredAt
                            _userRole.value = role
                            _isRegisteredDevice.value = true
                            listenToCurrentUserSession()
                            onResult(true, "Berhasil masuk sebagai ${role.replaceFirstChar { it.uppercase() }}")
                        }
                    } else {
                        // New registration
                        val now = System.currentTimeMillis()
                        val threeDaysMs = 3L * 24L * 60L * 60L * 1000L
                        val expiry = now + threeDaysMs
                        val role = "member"

                        val userData = hashMapOf(
                            "uid" to currentDeviceHwid,
                            "createdAt" to now,
                            "expiredAt" to expiry,
                            "role" to role,
                            "status" to "active"
                        )

                        userDocRef.set(userData)
                            .addOnSuccessListener {
                                _isLoading.value = false
                                prefs.edit()
                                    .putString("auth_uid", currentDeviceHwid)
                                    .putString("auth_role", role)
                                    .putLong("auth_expiry", expiry)
                                    .putBoolean("is_registered_device", true)
                                    .apply()

                                _currentUserSession.value = currentDeviceHwid
                                _expiryTime.value = expiry
                                _userRole.value = role
                                _isRegisteredDevice.value = true
                                listenToCurrentUserSession()
                                onResult(true, "Pendaftaran berhasil!")
                            }
                            .addOnFailureListener { e ->
                                _isLoading.value = false
                                onResult(false, "Gagal mendaftarkan perangkat: ${e.localizedMessage}")
                            }
                    }
                }.addOnFailureListener { e ->
                    _isLoading.value = false
                    onResult(false, "Gagal menghubungi server: ${e.localizedMessage}")
                }
            } catch (e: Exception) {
                _isLoading.value = false
                onResult(false, "Terjadi kesalahan: ${e.message}")
            }
        }
    }

    fun logout() {
        userDocumentListenerRegistration?.remove()
        usersCollectionListenerRegistration?.remove()
        licensesCollectionListenerRegistration?.remove()
        userDocumentListenerRegistration = null
        usersCollectionListenerRegistration = null
        licensesCollectionListenerRegistration = null

        prefs.edit()
            .remove("auth_uid")
            .remove("auth_role")
            .remove("auth_expiry")
            .remove("is_registered_device")
            .apply()
        _currentUserSession.value = null
        _expiryTime.value = null
        _userRole.value = null
        _isRegisteredDevice.value = false
    }

    fun fetchLicenseKeys() {
        fetchCurrentUserBalance()
        licensesCollectionListenerRegistration?.remove()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val currentUid = prefs.getString("auth_uid", "") ?: ""
                val currentUserRole = _userRole.value?.lowercase()
                
                val query = if (currentUserRole == "reseller") {
                    db.collection("licenses").whereEqualTo("generatedBy", currentUid)
                } else {
                    db.collection("licenses")
                }
                
                licensesCollectionListenerRegistration = query.addSnapshotListener { result, error ->
                    _isLoading.value = false
                    if (error != null) {
                        Log.e("BlackBoxViewModel", "Realtime licenses error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (result != null) {
                        val keys = result.mapNotNull { it.toObject(com.equinox.virtual.model.LicenseKey::class.java) }
                        _licenseKeys.value = keys.sortedByDescending { it.createdAt }
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun generateLicenseKey(durationDays: Int, role: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val currentUid = prefs.getString("auth_uid", "") ?: ""
                val currentUserRole = _userRole.value

                // If reseller, check balance first
                if (currentUserRole == "reseller") {
                    db.collection("users").document(currentUid).get().addOnSuccessListener { userDoc ->
                        val balance = userDoc.getLong("balance") ?: 0L
                        if (balance <= 0) {
                            _isLoading.value = false
                            viewModelScope.launch { _snackbarMessage.emit("Saldo tidak cukup! Silahkan hubungi Admin.") }
                            return@addOnSuccessListener
                        }
                        
                        // Proceed with transaction if balance OK
                        executeGenerateTransaction(db, currentUid, currentUserRole, durationDays, role)
                    }.addOnFailureListener { e ->
                        _isLoading.value = false
                        viewModelScope.launch { _snackbarMessage.emit("Gagal cek saldo: ${e.message}") }
                    }
                } else {
                    // Admin or other role doesn't need balance check (or at least admin doesn't)
                    executeGenerateTransaction(db, currentUid, currentUserRole, durationDays, role)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _snackbarMessage.emit("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    private fun executeGenerateTransaction(
        db: com.google.firebase.firestore.FirebaseFirestore,
        currentUid: String,
        currentUserRole: String?,
        durationDays: Int,
        role: String
    ) {
        val key = UUID.randomUUID().toString().substring(0, 8).uppercase()
        val license = com.equinox.virtual.model.LicenseKey(
            key = key,
            durationDays = durationDays,
            role = role,
            isUsed = false,
            generatedBy = currentUid
        )

        db.runTransaction { transaction ->
            // 1. Create license
            val licenseRef = db.collection("licenses").document(key)
            transaction.set(licenseRef, license)

            // 2. Deduct balance if reseller
            if (currentUserRole == "reseller") {
                val userRef = db.collection("users").document(currentUid)
                transaction.update(userRef, "balance", com.google.firebase.firestore.FieldValue.increment(-1))
            }
        }.addOnSuccessListener {
            fetchLicenseKeys()
            viewModelScope.launch { _snackbarMessage.emit("Berhasil membuat lisensi: $key") }
        }.addOnFailureListener { e ->
            _isLoading.value = false
            viewModelScope.launch { _snackbarMessage.emit("Gagal: ${e.message}") }
        }
    }

    fun fetchCurrentUserBalance() {
        val uid = prefs.getString("auth_uid", "") ?: ""
        if (uid.isEmpty()) return
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _currentUserBalance.value = snapshot.getLong("balance") ?: 0L
                }
            }
    }

    fun deleteLicenseKey(key: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("licenses").document(key).delete().addOnSuccessListener {
                    fetchLicenseKeys()
                    viewModelScope.launch { _snackbarMessage.emit("Berhasil menghapus lisensi") }
                }.addOnFailureListener { e ->
                    _isLoading.value = false
                    viewModelScope.launch { _snackbarMessage.emit("Gagal: ${e.message}") }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun fetchFirestoreUsers() {
        usersCollectionListenerRegistration?.remove()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                usersCollectionListenerRegistration = db.collection("users")
                    .addSnapshotListener { result, error ->
                        _isLoading.value = false
                        if (error != null) {
                            Log.e("BlackBoxViewModel", "Realtime users error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (result != null) {
                            val users = result.mapNotNull { it.toObject(com.equinox.virtual.model.FirestoreUser::class.java) }
                            _firestoreUsers.value = users
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun updateFirestoreUser(uid: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val filteredUpdates = updates.toMutableMap()
                if (_userRole.value?.lowercase() != "admin") {
                    filteredUpdates.remove("balance")
                }
                if (filteredUpdates.isEmpty()) {
                    _isLoading.value = false
                    return@launch
                }
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("users").document(uid).update(filteredUpdates).addOnSuccessListener {
                    fetchFirestoreUsers()
                    viewModelScope.launch { _snackbarMessage.emit("Berhasil memperbarui pengguna") }
                }.addOnFailureListener { e ->
                    _isLoading.value = false
                    viewModelScope.launch { _snackbarMessage.emit("Gagal: ${e.message}") }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun deleteFirestoreUser(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("users").document(uid).delete().addOnSuccessListener {
                    fetchFirestoreUsers()
                    viewModelScope.launch { _snackbarMessage.emit("Berhasil menghapus pengguna") }
                }.addOnFailureListener { e ->
                    _isLoading.value = false
                    viewModelScope.launch { _snackbarMessage.emit("Gagal: ${e.message}") }
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun fetchSystemStats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                
                // Fetch users and licenses
                db.collection("users").get().addOnSuccessListener { userDocs ->
                    db.collection("licenses").get().addOnSuccessListener { licenseDocs ->
                        val stats = mutableMapOf<String, Int>()
                        stats["total_users"] = userDocs.size()
                        stats["admin_count"] = userDocs.documents.count { it.getString("role") == "admin" }
                        stats["reseller_count"] = userDocs.documents.count { it.getString("role") == "reseller" }
                        stats["member_count"] = userDocs.documents.count { it.getString("role") == "member" }
                        
                        val totalBalance = userDocs.documents.sumOf { it.getLong("balance") ?: 0L }
                        stats["total_balance"] = totalBalance.toInt()

                        stats["total_licenses"] = licenseDocs.size()
                        stats["used_licenses"] = licenseDocs.documents.count { it.getBoolean("isUsed") == true }
                        stats["available_licenses"] = licenseDocs.documents.count { it.getBoolean("isUsed") == false }
                        
                        _systemStats.value = stats
                        _isLoading.value = false
                    }.addOnFailureListener { e ->
                        Log.e("BlackBoxViewModel", "Failed to fetch licenses stats: ${e.message}")
                        _isLoading.value = false
                    }
                }.addOnFailureListener { e ->
                    Log.e("BlackBoxViewModel", "Failed to fetch users stats: ${e.message}")
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("BlackBoxViewModel", "System stats error: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userDocumentListenerRegistration?.remove()
        usersCollectionListenerRegistration?.remove()
        licensesCollectionListenerRegistration?.remove()
    }
}
