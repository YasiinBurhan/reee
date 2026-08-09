package com.equinox.virtual.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.equinox.virtual.model.FirestoreUser
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    users: List<FirestoreUser>,
    isLoading: Boolean,
    currentUserRole: String? = null,
    onBack: () -> Unit,
    onUpdateUser: (String, Map<String, Any>) -> Unit,
    onDeleteUser: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf<FirestoreUser?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(1) }
    val itemsPerPage = 10

    val filteredUsers = users.filter { 
        it.uid.contains(searchQuery, ignoreCase = true) || 
        it.role.contains(searchQuery, ignoreCase = true)
    }
    
    val totalPages = (filteredUsers.size + itemsPerPage - 1) / itemsPerPage
    val paginatedUsers = filteredUsers.drop((currentPage - 1) * itemsPerPage).take(itemsPerPage)

    // Reset page when search changes
    LaunchedEffect(searchQuery) {
        currentPage = 1
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Kelola Pengguna") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Cari UID atau Role...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && users.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filteredUsers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "Belum ada data pengguna" else "Tidak ada hasil pencarian",
                        color = Color.Gray
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(paginatedUsers) { user ->
                            UserCard(
                                user = user,
                                onEdit = { showEditDialog = user },
                                onDelete = { onDeleteUser(user.uid) }
                            )
                        }
                    }
                    
                    if (totalPages > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentPage > 1) currentPage-- },
                                enabled = currentPage > 1
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                            }
                            
                            Text(
                                text = "Halaman $currentPage dari $totalPages",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            IconButton(
                                onClick = { if (currentPage < totalPages) currentPage++ },
                                enabled = currentPage < totalPages
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                            }
                        }
                    }
                }
            }
        }

        showEditDialog?.let { user ->
            EditUserDialog(
                user = user,
                currentUserRole = currentUserRole,
                onDismiss = { showEditDialog = null },
                onConfirm = { updates ->
                    onUpdateUser(user.uid, updates)
                    showEditDialog = null
                }
            )
        }
    }
}

@Composable
fun UserCard(
    user: FirestoreUser,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.uid,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (user.role) {
                                "admin" -> MaterialTheme.colorScheme.errorContainer
                                "reseller" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = user.role.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (user.role) {
                                    "admin" -> MaterialTheme.colorScheme.onErrorContainer
                                    "reseller" -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (user.expiredAt > System.currentTimeMillis()) "Aktif" else "Expired",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.expiredAt > System.currentTimeMillis()) Color(0xFF4CAF50) else Color.Red
                        )
                        if (user.role == "reseller") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Saldo: ${user.balance}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
            )
            
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            Text(
                text = "Expired: ${sdf.format(Date(user.expiredAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EditUserDialog(
    user: FirestoreUser,
    currentUserRole: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var role by remember { mutableStateOf(user.role) }
    var daysToAdd by remember { mutableStateOf("0") }
    var balanceToAdd by remember { mutableStateOf("0") }

    val isAdmin = currentUserRole?.lowercase() == "admin"
    val isTargetReseller = role == "reseller" || user.role == "reseller"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Pengguna") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("UID: ${user.uid}", style = MaterialTheme.typography.bodySmall)
                
                Text("Pilih Role:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("member", "reseller").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.uppercase()) }
                        )
                    }
                }
                
                if (user.role == "admin") {
                    Text(
                        text = "Role Admin tidak dapat diubah di sini.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                OutlinedTextField(
                    value = daysToAdd,
                    onValueChange = { if (it.all { char -> char.isDigit() }) daysToAdd = it },
                    label = { Text("Tambah Masa Aktif (Hari)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                if (isTargetReseller) {
                    if (isAdmin) {
                        OutlinedTextField(
                            value = balanceToAdd,
                            onValueChange = { if (it.all { char -> char.isDigit() }) balanceToAdd = it },
                            label = { Text("Tambah Saldo Reseller (Credits)") },
                            supportingText = { Text("Hanya Admin yang dapat menambah saldo reseller") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Penambahan saldo reseller hanya dapat dilakukan oleh Admin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updates = mutableMapOf<String, Any>()
                if (user.role != "admin") {
                    updates["role"] = role
                }
                val days = daysToAdd.toLongOrNull() ?: 0L
                if (days > 0) {
                    val currentExpiry = if (user.expiredAt > System.currentTimeMillis()) user.expiredAt else System.currentTimeMillis()
                    val newExpiry = currentExpiry + (days * 24 * 60 * 60 * 1000)
                    updates["expiredAt"] = newExpiry
                }
                if (isAdmin) {
                    val balanceAdd = balanceToAdd.toLongOrNull() ?: 0L
                    if (balanceAdd > 0) {
                        updates["balance"] = com.google.firebase.firestore.FieldValue.increment(balanceAdd)
                    }
                }
                onConfirm(updates)
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
