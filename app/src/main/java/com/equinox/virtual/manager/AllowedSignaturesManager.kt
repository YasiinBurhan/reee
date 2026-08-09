package com.equinox.virtual.manager

import android.content.SharedPreferences
import android.util.Log
import com.equinox.virtual.model.AllowedSignature
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AllowedSignaturesManager(
    private val prefs: SharedPreferences,
    private val getDbFunc: () -> FirebaseFirestore
) {
    private val _allowedSignatures = MutableStateFlow<Set<String>>(
        prefs.getStringSet("cached_allowed_signatures", emptySet()) ?: emptySet()
    )
    val allowedSignatures: StateFlow<Set<String>> = _allowedSignatures.asStateFlow()

    private val _allowedSignatureList = MutableStateFlow<List<AllowedSignature>>(emptyList())
    val allowedSignatureList: StateFlow<List<AllowedSignature>> = _allowedSignatureList.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    fun listenToAllowedSignatures(onComplete: (() -> Unit)? = null) {
        listenerRegistration?.remove()
        try {
            val db = getDbFunc()
            listenerRegistration = db.collection("allowed_signatures")
                .addSnapshotListener { snapshot, error ->
                    onComplete?.invoke()
                    if (error != null) {
                        Log.e("AllowedSignaturesManager", "Error listening to allowed_signatures: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.mapNotNull { doc ->
                            doc.toObject(AllowedSignature::class.java)
                        }
                        val sortedList = list.sortedByDescending { it.addedAt }
                        val sigSet = sortedList.map { it.signature.uppercase().trim() }.toSet()

                        _allowedSignatureList.value = sortedList
                        _allowedSignatures.value = sigSet

                        // Cache in SharedPreferences
                        prefs.edit().putStringSet("cached_allowed_signatures", sigSet).apply()
                    } else {
                        _allowedSignatureList.value = emptyList()
                        _allowedSignatures.value = emptySet()
                        prefs.edit().remove("cached_allowed_signatures").apply()
                    }
                }
        } catch (e: Exception) {
            Log.e("AllowedSignaturesManager", "Failed to setup allowed_signatures listener: ${e.message}")
            onComplete?.invoke()
        }
    }

    fun addAllowedSignature(
        signature: String,
        description: String,
        addedBy: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanSig = signature.trim().uppercase()
        val cleanDesc = description.trim()
        if (cleanSig.isEmpty()) {
            onFailure("Signature SHA-256 tidak boleh kosong")
            return
        }

        try {
            val db = getDbFunc()
            val item = AllowedSignature(
                signature = cleanSig,
                description = cleanDesc,
                addedAt = System.currentTimeMillis(),
                addedBy = addedBy
            )
            db.collection("allowed_signatures").document(cleanSig).set(item)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure("Gagal menambahkan signature: ${e.message}")
                }
        } catch (e: Exception) {
            onFailure("Error: ${e.message}")
        }
    }

    fun deleteAllowedSignature(
        signature: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanSig = signature.trim().uppercase()
        try {
            val db = getDbFunc()
            db.collection("allowed_signatures").document(cleanSig).delete()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure("Gagal menghapus signature: ${e.message}")
                }
        } catch (e: Exception) {
            onFailure("Error: ${e.message}")
        }
    }

    fun clearListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
