package com.equinox.virtual.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    var showQuickAddDialog by remember { mutableStateOf<FirestoreUser?>(null) }
    var showTopUpDialog by remember { mutableStateOf<FirestoreUser?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(1) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val itemsPerPage = 10

    val isAdmin = currentUserRole?.lowercase() == "admin"
    val currentTime = System.currentTimeMillis()

    val activeCount = remember(users, currentTime) {
        users.count { 
            val role = it.role.lowercase()
            role != "reseller" && role != "admin" && it.expiredAt > currentTime 
        }
    }
    val inactiveCount = remember(users, currentTime) {
        users.count { 
            val role = it.role.lowercase()
            role != "reseller" && role != "admin" && it.expiredAt <= currentTime 
        }
    }
    val resellerCount = remember(users) {
        users.count { it.role.lowercase() == "reseller" }
    }

    val tabTitles = if (isAdmin) {
        listOf("Member Aktif ($activeCount)", "Tidak Aktif ($inactiveCount)", "Reseller ($resellerCount)")
    } else {
        listOf("Member Aktif ($activeCount)", "Tidak Aktif ($inactiveCount)")
    }

    val tabFilteredUsers = users.filter { user ->
        val role = user.role.lowercase()
        val isSpecial = role == "reseller" || role == "admin"
        val isReseller = role == "reseller"
        val isActive = user.expiredAt > currentTime

        if (isAdmin) {
            when (selectedTabIndex) {
                0 -> !isSpecial && isActive
                1 -> !isSpecial && !isActive
                2 -> isReseller
                else -> true
            }
        } else {
            when (selectedTabIndex) {
                0 -> !isSpecial && isActive
                1 -> !isSpecial && !isActive
                else -> true
            }
        }
    }

    val filteredUsers = tabFilteredUsers.filter { 
        it.uid.contains(searchQuery, ignoreCase = true) || 
        it.role.contains(searchQuery, ignoreCase = true)
    }
    
    val totalPages = ((filteredUsers.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
    val paginatedUsers = filteredUsers.drop((currentPage - 1) * itemsPerPage).take(itemsPerPage)

    // Reset page when search or tab changes
    LaunchedEffect(searchQuery, selectedTabIndex) {
        currentPage = 1
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Kelola Pengguna") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
                TabRow(
                    selectedTabIndex = selectedTabIndex.coerceIn(0, tabTitles.size - 1)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
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
                                currentUserRole = currentUserRole,
                                onDelete = { onDeleteUser(user.uid) },
                                onQuickAdd = { showQuickAddDialog = user },
                                onPromote = {
                                    onUpdateUser(user.uid, mapOf(
                                        "role" to "reseller",
                                        "balance" to 250000L
                                    ))
                                },
                                onTopUp = { showTopUpDialog = user }
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

        showQuickAddDialog?.let { user ->
            QuickAddDaysDialog(
                user = user,
                currentUserRole = currentUserRole,
                onDismiss = { showQuickAddDialog = null },
                onConfirm = { days ->
                    val currentExpiry = if (user.expiredAt > System.currentTimeMillis()) user.expiredAt else System.currentTimeMillis()
                    val newExpiry = currentExpiry + (days * 24 * 60 * 60 * 1000)
                    onUpdateUser(user.uid, mapOf(
                        "expiredAt" to newExpiry,
                        "daysAdded" to days
                    ))
                    showQuickAddDialog = null
                }
            )
        }

        showTopUpDialog?.let { user ->
            TopUpBalanceDialog(
                user = user,
                onDismiss = { showTopUpDialog = null },
                onConfirm = { amount ->
                    onUpdateUser(user.uid, mapOf(
                        "balance" to com.google.firebase.firestore.FieldValue.increment(amount)
                    ))
                    showTopUpDialog = null
                }
            )
        }
    }
}

@Composable
fun QuickAddDaysDialog(
    user: FirestoreUser,
    currentUserRole: String?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedDays by remember { mutableLongStateOf(30L) }
    val isReseller = currentUserRole?.lowercase() == "reseller"

    val cost = when {
        selectedDays >= 365 -> 1500000L
        selectedDays >= 30 -> 150000L
        selectedDays >= 7 -> 40000L
        selectedDays >= 3 -> 20000L
        else -> 10000L * selectedDays
    }

    val currencyFormatter = java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aktivasi Cepat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tambah masa aktif untuk ${user.uid}")
                
                if (isReseller) {
                    Text(
                        text = "Biaya: ${currencyFormatter.format(cost)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1L, 3L, 7L, 30L).forEach { days ->
                        FilterChip(
                            selected = selectedDays == days,
                            onClick = { selectedDays = days },
                            label = { Text("${days}H") }
                        )
                    }
                }
                
                Text(
                    text = "Masa aktif akan dihitung dari waktu sekarang atau waktu expired terakhir (mana yang lebih baru).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedDays) }) {
                Text("Tambahkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun UserCard(
    user: FirestoreUser,
    currentUserRole: String?,
    onDelete: () -> Unit,
    onQuickAdd: () -> Unit,
    onPromote: () -> Unit,
    onTopUp: () -> Unit
) {
    val isAdmin = currentUserRole?.lowercase() == "admin"
    
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
                                text = "Saldo: Rp ${user.balance}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                if (isAdmin && user.role.lowercase() == "member") {
                    IconButton(onClick = onPromote) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd, 
                            contentDescription = "Angkat Reseller", 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (isAdmin && user.role == "reseller") {
                    IconButton(onClick = onTopUp) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet, 
                            contentDescription = "Top Up Saldo", 
                            tint = Color(0xFFFF9800)
                        )
                    }
                }

                IconButton(onClick = onQuickAdd) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Quick Add", tint = Color(0xFF4CAF50))
                }

                if (isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
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
            )
        }
    }
}

@Composable
fun TopUpBalanceDialog(
    user: FirestoreUser,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var amountText by remember { mutableStateOf("100000") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Top Up Saldo Reseller") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tambah saldo untuk ${user.uid}")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("Jumlah Saldo (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50000L, 100000L, 500000L, 1000000L).forEach { amount ->
                        FilterChip(
                            selected = amountText == amount.toString(),
                            onClick = { amountText = amount.toString() },
                            label = { Text("${amount/1000}K") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > 0) onConfirm(amount)
                }
            ) {
                Text("Top Up")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
