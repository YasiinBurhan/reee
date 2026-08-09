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

    private val _currentUserSession = MutableStateFlow<String?>(prefs.getString("auth_username", null))
    val currentUserSession: StateFlow<String?> = _currentUserSession.asStateFlow()

    private val _isCheckingSession = MutableStateFlow(true)
    val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    private val _expiryTime = MutableStateFlow<Long?>(
        prefs.getLong("auth_expiry", 0L).let { if (it == 0L) null else it }
    )
    val expiryTime: StateFlow<Long?> = _expiryTime.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(prefs.getString("auth_role", null))
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    private val _isRegisteredDevice = MutableStateFlow(prefs.getBoolean("is_registered_device", false))
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
        val savedAuth = prefs.getString("auth_uid", null)
        if (savedAuth != null) {
            listenToCurrentUserSession(onRoleChanged, onExpired)
        }
        _isCheckingSession.value = false
    }

    fun authenticateDevice(
        onRoleChanged: (String) -> Unit,
        onExpired: () -> Unit,
        onResult: (Boolean, String) -> Unit
    ) {
        val currentDeviceHwid = EQuinoxApp.getDeviceHwid()
        try {
            val db = getFirestoreDb()
            val userDocRef = db.collection("users").document(currentDeviceHwid)

            userDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val expiredAt = document.getLong("expiredAt") ?: 0L
                    val role = document.getString("role") ?: "member"

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
                        listenToCurrentUserSession(onRoleChanged, onExpired)
                        onResult(true, "Berhasil masuk sebagai ${role.replaceFirstChar { it.uppercase() }}")
                    }
                } else {
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
                            listenToCurrentUserSession(onRoleChanged, onExpired)
                            onResult(true, "Pendaftaran berhasil!")
                        }
                        .addOnFailureListener { e ->
                            onResult(false, "Gagal mendaftarkan perangkat: ${e.localizedMessage}")
                        }
                }
            }.addOnFailureListener { e ->
                onResult(false, "Gagal menghubungi server: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
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
