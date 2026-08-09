package com.equinox.virtual.manager

import android.content.SharedPreferences
import android.util.Log
import com.equinox.virtual.model.LicenseKey
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class LicenseAndStatsManager(
    private val prefs: SharedPreferences,
    private val getDbFunc: () -> FirebaseFirestore
) {

    private val _licenseKeys = MutableStateFlow<List<LicenseKey>>(emptyList())
    val licenseKeys: StateFlow<List<LicenseKey>> = _licenseKeys.asStateFlow()

    private val _systemStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val systemStats: StateFlow<Map<String, Int>> = _systemStats.asStateFlow()

    private var licensesCollectionListenerRegistration: ListenerRegistration? = null

    fun fetchLicenseKeys(userRole: String?, onComplete: () -> Unit) {
        licensesCollectionListenerRegistration?.remove()
        try {
            val db = getDbFunc()
            val currentUid = prefs.getString("auth_uid", "") ?: ""
            val currentUserRole = userRole?.lowercase()

            val query = if (currentUserRole == "reseller") {
                db.collection("licenses").whereEqualTo("generatedBy", currentUid)
            } else {
                db.collection("licenses")
            }

            licensesCollectionListenerRegistration = query.addSnapshotListener { result, error ->
                onComplete()
                if (error != null) {
                    Log.e("LicenseAndStatsManager", "Realtime licenses error: ${error.message}")
                    return@addSnapshotListener
                }
                if (result != null) {
                    val keys = result.mapNotNull { it.toObject(LicenseKey::class.java) }
                    _licenseKeys.value = keys.sortedByDescending { it.createdAt }
                }
            }
        } catch (e: Exception) {
            onComplete()
        }
    }

    fun generateLicenseKey(
        userRole: String?,
        durationDays: Int,
        role: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val db = getDbFunc()
            val currentUid = prefs.getString("auth_uid", "") ?: ""
            val currentUserRole = userRole

            if (currentUserRole == "reseller") {
                db.collection("users").document(currentUid).get().addOnSuccessListener { userDoc ->
                    val balance = userDoc.getLong("balance") ?: 0L
                    if (balance <= 0) {
                        onFailure("Saldo tidak cukup! Silahkan hubungi Admin.")
                        return@addOnSuccessListener
                    }
                    executeGenerateTransaction(db, currentUid, currentUserRole, durationDays, role, onSuccess, onFailure)
                }.addOnFailureListener { e ->
                    onFailure("Gagal cek saldo: ${e.message}")
                }
            } else {
                executeGenerateTransaction(db, currentUid, currentUserRole, durationDays, role, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            onFailure("Terjadi kesalahan: ${e.message}")
        }
    }

    private fun executeGenerateTransaction(
        db: FirebaseFirestore,
        currentUid: String,
        currentUserRole: String?,
        durationDays: Int,
        role: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val key = UUID.randomUUID().toString().substring(0, 8).uppercase()
        val license = LicenseKey(
            key = key,
            durationDays = durationDays,
            role = role,
            isUsed = false,
            generatedBy = currentUid
        )

        db.runTransaction { transaction ->
            val licenseRef = db.collection("licenses").document(key)
            transaction.set(licenseRef, license)

            if (currentUserRole == "reseller") {
                val userRef = db.collection("users").document(currentUid)
                transaction.update(userRef, "balance", FieldValue.increment(-1))
            }
        }.addOnSuccessListener {
            onSuccess(key)
        }.addOnFailureListener { e ->
            onFailure("Gagal: ${e.message}")
        }
    }

    fun deleteLicenseKey(
        key: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val db = getDbFunc()
            db.collection("licenses").document(key).delete()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure("Gagal: ${e.message}")
                }
        } catch (e: Exception) {
            onFailure(e.message ?: "Error")
        }
    }

    fun fetchSystemStats(onComplete: () -> Unit) {
        try {
            val db = getDbFunc()
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
                    onComplete()
                }.addOnFailureListener { e ->
                    Log.e("LicenseAndStatsManager", "Failed to fetch licenses stats: ${e.message}")
                    onComplete()
                }
            }.addOnFailureListener { e ->
                Log.e("LicenseAndStatsManager", "Failed to fetch users stats: ${e.message}")
                onComplete()
            }
        } catch (e: Exception) {
            Log.e("LicenseAndStatsManager", "System stats error: ${e.message}")
            onComplete()
        }
    }

    fun clearListeners() {
        licensesCollectionListenerRegistration?.remove()
        licensesCollectionListenerRegistration = null
    }
}
