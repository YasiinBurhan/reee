package com.equinox.virtual.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equinox.virtual.ui.theme.*

@Composable
fun ProfilTabScreen(
    currentUserId: Int,
    userList: List<Int>,
    deviceSpoofingEnabled: Boolean = true,
    gmsProxyEnabled: Boolean = true,
    storageIsolationEnabled: Boolean = true,
    rootHideEnabled: Boolean = true,
    onToggleDeviceSpoofing: (Boolean) -> Unit = {},
    onToggleGmsProxy: (Boolean) -> Unit = {},
    onToggleStorageIsolation: (Boolean) -> Unit = {},
    onToggleRootHide: (Boolean) -> Unit = {},
    onSelectUser: (Int) -> Unit,
    onAddUser: () -> Unit,
    onDeleteUser: (Int) -> Unit,
    onRefreshAll: () -> Unit,
    onLogout: () -> Unit = {},
    currentUserSession: String? = null,
    expiryTime: Long? = null,
    userRole: String? = null
) {
    val mContext = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // iOS Header
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iOSBlue.copy(alpha = 0.1f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = iOSBlue,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUserSession ?: com.equinox.virtual.EQuinoxApp.getDeviceHwid(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        val roleStr = (userRole ?: "MEMBER").uppercase()
                        Text(
                            text = roleStr,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (roleStr == "ADMIN") iOSRed else iOSBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val expiryStr = remember(expiryTime, userRole) {
                            val roleLower = userRole?.lowercase() ?: "member"
                            if (roleLower == "admin" || roleLower == "reseller") {
                                "Masa aktif: Selamanya"
                            } else if (expiryTime != null && expiryTime > 0L) {
                                val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                "Masa aktif: " + sdf.format(java.util.Date(expiryTime))
                            } else {
                                "Masa aktif: Selamanya"
                            }
                        }
                        Text(
                            text = expiryStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSSystemGray
                        )
                    }
                }
            }
        }

        // ID Section
        item {
            SettingsGroup(title = "IDENTITAS PERANGKAT") {
                SettingsRow(
                    title = "Device ID / HWID",
                    subtitle = if (currentUserId == 0) com.equinox.virtual.EQuinoxApp.getDeviceHwid() else "User Space $currentUserId",
                    icon = Icons.Default.Fingerprint,
                    iconBackground = iOSBlue,
                    onClick = {
                        if (currentUserId == 0) {
                            val clipboard = mContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("UID", com.equinox.virtual.EQuinoxApp.getDeviceHwid())
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(mContext, "ID disalin ke papan klip!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // Sandbox Settings
        item {
            SettingsGroup(title = "KONFIGURASI SANDBOX") {
                SettingsSwitchRow(
                    title = "Pemalsuan Identitas",
                    subtitle = "IMEI, Android ID, MAC Address",
                    icon = Icons.Default.Security,
                    iconBackground = iOSGreen,
                    checked = deviceSpoofingEnabled,
                    onCheckedChange = onToggleDeviceSpoofing
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = "Layanan Google Play",
                    subtitle = "GMS Proxy & Stub",
                    icon = Icons.Default.Cloud,
                    iconBackground = iOSLightBlue,
                    checked = gmsProxyEnabled,
                    onCheckedChange = onToggleGmsProxy
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = "Isolasi Data",
                    subtitle = "Pemisahan folder virtual",
                    icon = Icons.Default.Storage,
                    iconBackground = iOSOrange,
                    checked = storageIsolationEnabled,
                    onCheckedChange = onToggleStorageIsolation
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = "Anti-Deteksi",
                    subtitle = "Sembunyikan Root & Hook",
                    icon = Icons.Default.Shield,
                    iconBackground = iOSIndigo,
                    checked = rootHideEnabled,
                    onCheckedChange = onToggleRootHide
                )
            }
        }

        // Actions
        item {
            SettingsGroup(title = "TINDAKAN MESIN") {
                SettingsRow(
                    title = "Muat Ulang Sandbox",
                    subtitle = "Bersihkan cache & restart engine",
                    icon = Icons.Default.Refresh,
                    iconBackground = iOSIndigo,
                    onClick = onRefreshAll
                )
                SettingsDivider()
                SettingsRow(
                    title = "Keluar Akun",
                    subtitle = "Selesaikan sesi saat ini",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconBackground = iOSRed,
                    onClick = onLogout
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "EQuinox Virtual v1.0.0 (BETA)",
                    style = MaterialTheme.typography.labelSmall,
                    color = iOSSystemGray.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = iOSSystemGray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconBackground: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = iconBackground
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = iOSSystemGray)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = iOSSystemGray.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconBackground: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = iconBackground
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = iOSSystemGray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iOSGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp),
        thickness = 0.5.dp,
        color = iOSSystemGray5
    )
}
