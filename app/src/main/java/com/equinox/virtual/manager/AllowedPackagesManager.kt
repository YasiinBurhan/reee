package com.equinox.virtual.manager

import android.util.Log
import com.equinox.virtual.model.AllowedPackage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AllowedPackagesManager(
    private val getDbFunc: () -> FirebaseFirestore
) {

    private val _allowedPackages = MutableStateFlow<Set<String>>(emptySet())
    val allowedPackages: StateFlow<Set<String>> = _allowedPackages.asStateFlow()

    private val _allowedPackageList = MutableStateFlow<List<AllowedPackage>>(emptyList())
    val allowedPackageList: StateFlow<List<AllowedPackage>> = _allowedPackageList.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    fun listenToAllowedPackages(onComplete: (() -> Unit)? = null) {
        listenerRegistration?.remove()
        try {
            val db = getDbFunc()
            listenerRegistration = db.collection("allowed_packages")
                .addSnapshotListener { snapshot, error ->
                    onComplete?.invoke()
                    if (error != null) {
                        Log.e("AllowedPackagesManager", "Error listening to allowed_packages: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.mapNotNull { doc ->
                            doc.toObject(AllowedPackage::class.java)
                        }
                        _allowedPackageList.value = list.sortedByDescending { it.addedAt }
                        _allowedPackages.value = list.map { it.packageName }.toSet()
                    } else {
                        _allowedPackageList.value = emptyList()
                        _allowedPackages.value = emptySet()
                    }
                }
        } catch (e: Exception) {
            Log.e("AllowedPackagesManager", "Failed to setup allowed_packages listener: ${e.message}")
            onComplete?.invoke()
        }
    }

    fun addAllowedPackage(
        packageName: String,
        appName: String,
        addedBy: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanPkg = packageName.trim()
        val cleanName = appName.trim().ifEmpty { cleanPkg }
        if (cleanPkg.isEmpty()) {
            onFailure("Package Name tidak boleh kosong")
            return
        }

        try {
            val db = getDbFunc()
            val item = AllowedPackage(
                packageName = cleanPkg,
                appName = cleanName,
                addedAt = System.currentTimeMillis(),
                addedBy = addedBy
            )
            db.collection("allowed_packages").document(cleanPkg).set(item)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure("Gagal menambahkan: ${e.message}")
                }
        } catch (e: Exception) {
            onFailure("Error: ${e.message}")
        }
    }

    fun deleteAllowedPackage(
        packageName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val db = getDbFunc()
            db.collection("allowed_packages").document(packageName).delete()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure("Gagal menghapus: ${e.message}")
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
