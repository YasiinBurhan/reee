package com.equinox.virtual.manager

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import com.equinox.virtual.EQuinoxApp
import com.equinox.virtual.model.FirestoreUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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

    private val _expiryTime = MutableStateFlow<Long?>(null)
    val expiryTime: StateFlow<Long?> = _expiryTime.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    private val _isRegisteredDevice = MutableStateFlow(false)
    val isRegisteredDevice: StateFlow<Boolean> = _isRegisteredDevice.asStateFlow()

    private val _firestoreUsers = MutableStateFlow<List<FirestoreUser>>(emptyList())
    val firestoreUsers: StateFlow<List<FirestoreUser>> = _firestoreUsers.asStateFlow()

    private val _currentUserBalance = MutableStateFlow(0L)
    val currentUserBalance: StateFlow<Long> = _currentUserBalance.asStateFlow()

    private var userDocumentListenerRegistration: ListenerRegistration? = null
    private var usersCollectionListenerRegistration: ListenerRegistration? = null

    fun getFirestoreDb(): FirebaseFirestore {
        val app = EQuinoxApp.initFirebase(application)
        
        val db = try {
            if (app != null) {
                FirebaseFirestore.getInstance(app)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.e("AuthAndUserManager", "CRITICAL: Could not get Firestore instance: ${e.message}")
            // Fallback: try initializing Firebase one more time manually
            EQuinoxApp.initFirebase(application)
            FirebaseFirestore.getInstance()
        }
        
        try {
            // Disable persistence to avoid ashmem/SQLite issues in virtual spaces
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(com.google.firebase.firestore.MemoryCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
        } catch (e: Exception) {
            Log.w("AuthAndUserManager", "Failed to set Firestore settings: ${e.message}")
        }
        
        return db
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
        Log.d("AuthAndUserManager", "authenticateDevice started")
        _isCheckingSession.value = true
        val currentDeviceHwid = EQuinoxApp.getDeviceHwid()
        Log.d("AuthAndUserManager", "HWID: $currentDeviceHwid")
        
        try {
            android.widget.Toast.makeText(application, "Menghubungkan ke server...", android.widget.Toast.LENGTH_SHORT).show()
            val db = getFirestoreDb()
            Log.d("AuthAndUserManager", "Firestore instance obtained")
            db.collection("users").document(currentDeviceHwid).get()
                .addOnSuccessListener { snapshot ->
                    Log.d("AuthAndUserManager", "Firestore get success. Snapshot exists: ${snapshot?.exists()}")
                    try {
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

                            val isExpired = System.currentTimeMillis() > expiredAt && role == "member"

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
                                else -> if (isExpired) "Member (Kadaluwarsa)" else "Member"
                            }
                            onRoleChanged(roleDisplay)
                            _isCheckingSession.value = false
                            listenToCurrentUserSession(onRoleChanged, onExpired)

                            if (isExpired) {
                                onResult(true, "Masuk Berhasil. Masa aktif telah berakhir - Halaman Virtual Terkunci.")
                            } else {
                                onResult(true, "Selamat Datang Kembali!")
                            }
                        } else {
                            // New User Registration (Auto-register for now or handle trial)
                            android.widget.Toast.makeText(application, "Mendaftarkan perangkat baru...", android.widget.Toast.LENGTH_SHORT).show()
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
                    } catch (e: Exception) {
                        Log.e("AuthAndUserManager", "Error processing snapshot: ${e.message}", e)
                        _isCheckingSession.value = false
                        onResult(false, "Error data: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AuthAndUserManager", "Firestore get failure", e)
                    _isCheckingSession.value = false
                    onResult(false, "Gagal terhubung ke server: ${e.message}")
                    android.widget.Toast.makeText(application, "Firestore Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
        } catch (e: SecurityException) {
            Log.e("AuthAndUserManager", "SecurityException in authenticateDevice: ${e.message}", e)
            _isCheckingSession.value = false
            onResult(false, "Sistem Keamanan Menolak: ${e.message}")
            android.widget.Toast.makeText(application, "Security Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("AuthAndUserManager", "Critical error in authenticateDevice: ${e.message}", e)
            _isCheckingSession.value = false
            onResult(false, "Terjadi kesalahan sistem: ${e.message}")
            android.widget.Toast.makeText(application, "System Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
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
            val db = getFirestoreDb()
            val currentUid = prefs.getString("auth_uid", "") ?: ""
            val currentUserRole = _userRole.value?.lowercase() ?: "member"
            
            val filteredUpdates = updates.toMutableMap()
            
            // Security check: only admins can update balance or status to banned for others
            if (currentUserRole != "admin") {
                filteredUpdates.remove("balance")
                if (filteredUpdates.containsKey("status") && filteredUpdates["status"] == "banned") {
                    filteredUpdates.remove("status")
                }
            }

            if (filteredUpdates.isEmpty()) {
                onSuccess()
                return
            }

            // If a reseller is adding days (expiredAt updated), deduct balance based on days
            val isAddingDays = updates.containsKey("expiredAt")
            val daysAdded = updates["daysAdded"] as? Long ?: 0L
            
            if (currentUserRole == "reseller" && isAddingDays) {
                db.runTransaction { transaction ->
                    val resellerRef = db.collection("users").document(currentUid)
                    val targetUserRef = db.collection("users").document(uid)
                    
                    val resellerDoc = transaction.get(resellerRef)
                    val balance = resellerDoc.getLong("balance") ?: 0L
                    
                    val cost = when {
                        daysAdded >= 365 -> 1500000L // Yearly bulk discount maybe? Or just keep it.
                        daysAdded >= 30 -> 150000L
                        daysAdded >= 7 -> 40000L
                        daysAdded >= 3 -> 20000L
                        else -> 10000L * daysAdded // 10k per day for others
                    }
                    
                    if (balance < cost) {
                        throw Exception("Saldo tidak cukup! Biaya: Rp $cost, Saldo Anda: Rp $balance")
                    }
                    
                    val finalUpdates = filteredUpdates.toMutableMap()
                    finalUpdates.remove("daysAdded")
                    
                    transaction.update(targetUserRef, finalUpdates)
                    transaction.update(resellerRef, "balance", com.google.firebase.firestore.FieldValue.increment(-cost))
                }.addOnSuccessListener {
                    onSuccess()
                }.addOnFailureListener { e ->
                    onFailure(e.message ?: "Gagal memperbarui")
                }
            } else {
                // Admin or no days added by reseller
                val finalUpdates = filteredUpdates.toMutableMap()
                finalUpdates.remove("daysAdded")
                
                db.collection("users").document(uid).update(finalUpdates)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "Gagal memperbarui")
                    }
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
