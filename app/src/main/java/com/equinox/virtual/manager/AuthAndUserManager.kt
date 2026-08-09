package com.equinox.virtual.manager

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import com.equinox.virtual.EQuinoxApp
import com.equinox.virtual.model.FirestoreUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthAndUserManager(
    private val application: Application,
    private val prefs: SharedPreferences
) {

    private val _currentUserSession = MutableStateFlow<String?>(null)
    val currentUserSession: StateFlow<String?> = _currentUserSession.asStateFlow()

    private val _isCheckingSession = MutableStateFlow(false)
    val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    private val _expiryTime = MutableStateFlow<Long?>(System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000))
    val expiryTime: StateFlow<Long?> = _expiryTime.asStateFlow()

    private val _userRole = MutableStateFlow<String?>("admin")
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    private val _isRegisteredDevice = MutableStateFlow(true)
    val isRegisteredDevice: StateFlow<Boolean> = _isRegisteredDevice.asStateFlow()

    private val _firestoreUsers = MutableStateFlow<List<FirestoreUser>>(emptyList())
    val firestoreUsers: StateFlow<List<FirestoreUser>> = _firestoreUsers.asStateFlow()

    private val _currentUserBalance = MutableStateFlow(0L)
    val currentUserBalance: StateFlow<Long> = _currentUserBalance.asStateFlow()

    private var userDocumentListenerRegistration: ListenerRegistration? = null
    private var usersCollectionListenerRegistration: ListenerRegistration? = null

    fun getFirestoreDb(): FirebaseFirestore {
        val app = EQuinoxApp.initFirebase(application)
        return if (app != null) {
            FirebaseFirestore.getInstance(app)
        } else {
            FirebaseFirestore.getInstance()
        }
    }

    fun listenToCurrentUserSession(
        onRoleChanged: (String) -> Unit,
        onExpired: () -> Unit
    ) {
        val currentDeviceHwid = EQuinoxApp.getDeviceHwid()
        val savedAuth = prefs.getString("auth_uid", null)
        if (savedAuth.isNullOrEmpty()) return

        userDocumentListenerRegistration?.remove()
        try {
            val db = getFirestoreDb()
            userDocumentListenerRegistration = db.collection("users").document(currentDeviceHwid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AuthAndUserManager", "Realtime user listener error: ${error.message}")
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
                            val roleDisplay = when (role.lowercase()) {
                                "admin" -> "Administrator"
                                "reseller" -> "Reseller"
                                else -> "Member"
                            }
                            onRoleChanged(roleDisplay)
                        }

                        if (System.currentTimeMillis() > expiredAt && role == "member") {
                            logout()
                            onExpired()
                        }
                    } else if (_currentUserSession.value != null) {
                        logout()
                    }
                }
        } catch (e: Exception) {
            Log.e("AuthAndUserManager", "Error attaching user listener: ${e.message}")
        }
    }

    fun validateSession(onRoleChanged: (String) -> Unit, onExpired: () -> Unit) {
        _isCheckingSession.value = true
        val currentDeviceHwid = EQuinoxApp.getDeviceHwid()
        
        try {
            val db = getFirestoreDb()
            db.collection("users").document(currentDeviceHwid).get()
                .addOnSuccessListener { snapshot ->
                    _isCheckingSession.value = false
                    if (snapshot != null && snapshot.exists()) {
                        val expiredAt = snapshot.getLong("expiredAt") ?: 0L
                        val role = snapshot.getString("role") ?: "member"
                        val balance = snapshot.getLong("balance") ?: 0L
                        val status = snapshot.getString("status") ?: "active"

                        if (status == "banned") {
                            logout()
                            return@addOnSuccessListener
                        }

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

                        val roleDisplay = when (role.lowercase()) {
                            "admin" -> "Administrator"
                            "reseller" -> "Reseller"
                            else -> "Member"
                        }
                        onRoleChanged(roleDisplay)

                        if (System.currentTimeMillis() > expiredAt && role == "member") {
                            // Don't logout immediately in validateSession, just let the UI handle it or start listener
                            listenToCurrentUserSession(onRoleChanged, onExpired)
                        } else {
                            listenToCurrentUserSession(onRoleChanged, onExpired)
                        }
                    } else {
                        _isRegisteredDevice.value = false
                        _currentUserSession.value = null
                    }
                }
                .addOnFailureListener { e ->
                    _isCheckingSession.value = false
                    _isRegisteredDevice.value = false
                    Log.e("AuthAndUserManager", "Session validation failed: ${e.message}")
                }
        } catch (e: Exception) {
            _isCheckingSession.value = false
            Log.e("AuthAndUserManager", "Error in validateSession: ${e.message}")
        }
    }

    fun authenticateDevice(
        onRoleChanged: (String) -> Unit,
        onExpired: () -> Unit,
        onResult: (Boolean, String) -> Unit
    ) {
        _isCheckingSession.value = true
        val currentDeviceHwid = EQuinoxApp.getDeviceHwid()
        
        try {
            val db = getFirestoreDb()
            db.collection("users").document(currentDeviceHwid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        // User already exists, check status and expiry
                        val expiredAt = snapshot.getLong("expiredAt") ?: 0L
                        val role = snapshot.getString("role") ?: "member"
                        val status = snapshot.getString("status") ?: "active"

                        if (status == "banned") {
                            _isCheckingSession.value = false
                            onResult(false, "Perangkat Anda telah diblokir!")
                            return@addOnSuccessListener
                        }

                        if (System.currentTimeMillis() > expiredAt && role == "member") {
                            _isCheckingSession.value = false
                            onResult(false, "Masa aktif lisensi Anda telah berakhir!")
                        } else {
                            _currentUserSession.value = currentDeviceHwid
                            _expiryTime.value = expiredAt
                            _userRole.value = role
                            _isRegisteredDevice.value = true
                            
                            prefs.edit()
                                .putString("auth_uid", currentDeviceHwid)
                                .putLong("auth_expiry", expiredAt)
                                .putString("auth_role", role)
                                .putBoolean("is_registered_device", true)
                                .apply()

                            val roleDisplay = when (role.lowercase()) {
                                "admin" -> "Administrator"
                                "reseller" -> "Reseller"
                                else -> "Member"
                            }
                            onRoleChanged(roleDisplay)
                            _isCheckingSession.value = false
                            listenToCurrentUserSession(onRoleChanged, onExpired)
                            onResult(true, "Selamat Datang Kembali!")
                        }
                    } else {
                        // New User Registration (Auto-register for now or handle trial)
                        val newUser = com.equinox.virtual.model.FirestoreUser(
                            uid = currentDeviceHwid,
                            role = "member",
                            expiredAt = System.currentTimeMillis() + (24L * 60 * 60 * 1000), // 1 day trial
                            status = "active",
                            createdAt = System.currentTimeMillis()
                        )
                        
                        db.collection("users").document(currentDeviceHwid).set(newUser)
                            .addOnSuccessListener {
                                _currentUserSession.value = currentDeviceHwid
                                _expiryTime.value = newUser.expiredAt
                                _userRole.value = newUser.role
                                _isRegisteredDevice.value = true
                                
                                prefs.edit()
                                    .putString("auth_uid", currentDeviceHwid)
                                    .putLong("auth_expiry", newUser.expiredAt)
                                    .putString("auth_role", newUser.role)
                                    .putBoolean("is_registered_device", true)
                                    .apply()

                                onRoleChanged("Member (Trial)")
                                _isCheckingSession.value = false
                                listenToCurrentUserSession(onRoleChanged, onExpired)
                                onResult(true, "Pendaftaran Berhasil! Nikmati akses trial 24 jam.")
                            }
                            .addOnFailureListener { e ->
                                _isCheckingSession.value = false
                                onResult(false, "Pendaftaran Gagal: ${e.message}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    _isCheckingSession.value = false
                    onResult(false, "Gagal terhubung ke server: ${e.message}")
                }
        } catch (e: Exception) {
            _isCheckingSession.value = false
            onResult(false, "Terjadi kesalahan: ${e.message}")
        }
    }

    fun logout() {
        userDocumentListenerRegistration?.remove()
        usersCollectionListenerRegistration?.remove()
        userDocumentListenerRegistration = null
        usersCollectionListenerRegistration = null

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

    fun fetchCurrentUserBalance() {
        val uid = prefs.getString("auth_uid", "") ?: ""
        if (uid.isEmpty()) return

        getFirestoreDb()
            .collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _currentUserBalance.value = snapshot.getLong("balance") ?: 0L
                }
            }
    }

    fun fetchFirestoreUsers(onResult: (Boolean, String?) -> Unit) {
        usersCollectionListenerRegistration?.remove()
        try {
            val db = getFirestoreDb()
            usersCollectionListenerRegistration = db.collection("users")
                .addSnapshotListener { result, error ->
                    if (error != null) {
                        Log.e("AuthAndUserManager", "Realtime users error: ${error.message}")
                        onResult(false, error.message)
                        return@addSnapshotListener
                    }
                    if (result != null) {
                        val users = result.mapNotNull { it.toObject(FirestoreUser::class.java) }
                        _firestoreUsers.value = users
                        onResult(true, null)
                    }
                }
        } catch (e: Exception) {
            onResult(false, e.message)
        }
    }

    fun updateFirestoreUser(
        uid: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val filteredUpdates = updates.toMutableMap()
            if (_userRole.value?.lowercase() != "admin") {
                filteredUpdates.remove("balance")
            }
            if (filteredUpdates.isEmpty()) {
                onSuccess()
                return
            }
            val db = getFirestoreDb()
            db.collection("users").document(uid).update(filteredUpdates)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Gagal memperbarui")
                }
        } catch (e: Exception) {
            onFailure(e.message ?: "Error")
        }
    }

    fun deleteFirestoreUser(
        uid: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val db = getFirestoreDb()
            db.collection("users").document(uid).delete()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Gagal menghapus")
                }
        } catch (e: Exception) {
            onFailure(e.message ?: "Error")
        }
    }

    fun clearListeners() {
        userDocumentListenerRegistration?.remove()
        usersCollectionListenerRegistration?.remove()
        userDocumentListenerRegistration = null
        usersCollectionListenerRegistration = null
    }
}
