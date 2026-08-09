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

    private val _systemStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val systemStats: StateFlow<Map<String, Int>> = _systemStats.asStateFlow()

    fun fetchSystemStats(onComplete: () -> Unit) {
        try {
            val db = getDbFunc()
            db.collection("users").get().addOnSuccessListener { userDocs ->
                val stats = mutableMapOf<String, Int>()
                stats["total_users"] = userDocs.size()
                stats["admin_count"] = userDocs.documents.count { it.getString("role") == "admin" }
                stats["reseller_count"] = userDocs.documents.count { it.getString("role") == "reseller" }
                stats["member_count"] = userDocs.documents.count { it.getString("role") == "member" }

                val totalBalance = userDocs.documents.sumOf { it.getLong("balance") ?: 0L }
                stats["total_balance"] = totalBalance.toInt()

                _systemStats.value = stats
                onComplete()
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
    }
}
