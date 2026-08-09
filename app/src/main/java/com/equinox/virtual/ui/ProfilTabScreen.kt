package com.equinox.virtual.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    val mContext = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // User Profile Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = currentUserSession?.uppercase() ?: "PENGELOLA VIRTUAL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val expiryStr = remember(expiryTime) {
                            if (expiryTime != null && expiryTime > 0L) {
                                val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
                                "Aktif s/d: " + sdf.format(java.util.Date(expiryTime))
                            } else {
                                "Masa Aktif Tidak Terbatas"
                            }
                        }
                        Text(
                            text = expiryStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    if (currentUserId == 0) {
                                        val clipboard = mContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("UID", com.equinox.virtual.EQuinoxApp.getDeviceHwid())
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(mContext, "UID berhasil disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = if (currentUserId == 0) "UID: ${com.equinox.virtual.EQuinoxApp.getDeviceHwid()}" else "ID Ruang Pengguna: $currentUserId",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentUserId == 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Salin UID",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when(userRole) {
                                "admin" -> MaterialTheme.colorScheme.errorContainer
                                "reseller" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = (userRole ?: "MEMBER").uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when(userRole) {
                                    "admin" -> MaterialTheme.colorScheme.onErrorContainer
                                    "reseller" -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Virtual Engine Security & Fingerprint Settings
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Sandbox & Sidik Jari Perangkat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SwitchSettingRow(
                        title = "Pemalsuan Model Perangkat",
                        subtitle = "Acak Android ID, IMEI & alamat MAC untuk aplikasi kloning",
                        checked = deviceSpoofingEnabled,
                        onCheckedChange = onToggleDeviceSpoofing
                    )

                    SwitchSettingRow(
                        title = "Proksi Layanan Google Play",
                        subtitle = "Aktifkan perantara GMS Stub untuk kompatibilitas Login Google",
                        checked = gmsProxyEnabled,
                        onCheckedChange = onToggleGmsProxy
                    )

                    SwitchSettingRow(
                        title = "Isolasi Penyimpanan",
                        subtitle = "Isolasi file virtual dari penyimpanan utama perangkat",
                        checked = storageIsolationEnabled,
                        onCheckedChange = onToggleStorageIsolation
                    )

                    SwitchSettingRow(
                        title = "Sembunyikan Root & Hook Lingkungan",
                        subtitle = "Cegah aplikasi kloning mendeteksi ruang virtual",
                        checked = rootHideEnabled,
                        onCheckedChange = onToggleRootHide
                    )
                }
            }
        }

        // User Space Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Mode Ruang Pengguna",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (currentUserId == 0) "Perangkat Utama" else "Ruang Pengguna Utama ($currentUserId)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Lingkungan ruang isolasi terpisah",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        item {
            Button(
                onClick = onRefreshAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Muat Ulang Semua Ruang Virtual")
            }
        }

        item {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("logout_button")
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun")
            }
        }
    }
}
