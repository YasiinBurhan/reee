package com.equinox.virtual.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.equinox.virtual.viewmodel.BlackBoxViewModel

@Composable
fun SettingTabScreen(
    userRole: String? = null,
    viewModel: BlackBoxViewModel,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToAllowedPackages: () -> Unit,
    onNavigateToSystemStats: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pengaturan Lanjutan",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Konfigurasi khusus berdasarkan hak akses akun Anda",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Admin & Reseller Panel
        val isAdmin = userRole?.lowercase() == "admin"
        val isReseller = userRole?.lowercase() == "reseller"

        if (isAdmin || isReseller) {
            item { ProfileSectionTitle(if (isAdmin) "Panel Administrator" else "Panel Reseller") }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.People,
                    title = if (isAdmin) "Kelola Pengguna" else "Kelola Client",
                    subtitle = if (isAdmin) "Manajemen akun dan hak akses" else "Lihat dan kelola daftar pengguna",
                    onClick = onNavigateToUserManagement
                )
            }
            if (isAdmin) {
                item {
                    ProfileMenuItem(
                        icon = Icons.Default.Apps,
                        title = "APK Bisa Dikloning (Whitelist)",
                        subtitle = "Kelola daftar package APK yang diizinkan untuk dikloning",
                        onClick = onNavigateToAllowedPackages
                    )
                }

            }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.BarChart,
                    title = "Statistik Sistem",
                    subtitle = "Analitik pertumbuhan dan lisensi",
                    onClick = onNavigateToSystemStats
                )
            }
        }

        if (!isAdmin && !isReseller) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Anda tidak memiliki akses ke pengaturan ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            ProfileSectionTitle("Informasi")
            ProfileMenuItem(
                icon = Icons.Default.Info,
                title = "Tentang Aplikasi",
                subtitle = "Informasi versi dan lisensi"
            )
        }
    }
}
