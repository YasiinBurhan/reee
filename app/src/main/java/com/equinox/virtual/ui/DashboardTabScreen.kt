package com.equinox.virtual.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equinox.virtual.model.VirtualAppInfo
import com.equinox.virtual.ui.theme.*

@Composable
fun DashboardTabScreen(
    virtualApps: List<VirtualAppInfo>,
    userList: List<Int>,
    currentUserId: Int,
    onNavigateToVirtualSpace: () -> Unit,
    onNavigateToDownload: () -> Unit,
    onLaunchApp: (VirtualAppInfo) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // System Status Overview Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = iOSBlue
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Security",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "EQuinox Sandbox",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                val mContext = LocalContext.current
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            if (currentUserId == 0) {
                                                val clipboard = mContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("UID", com.equinox.virtual.EQuinoxApp.getDeviceHwid())
                                                clipboard.setPrimaryClip(clip)
                                                android.widget.Toast.makeText(mContext, "UID disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (currentUserId == 0) "ID: ${com.equinox.virtual.EQuinoxApp.getDeviceHwid()}" else "Ruang Virtual • $currentUserId",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (currentUserId == 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Salin UID",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onNavigateToVirtualSpace,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = iOSBlue
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Buka App", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onNavigateToDownload,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pasang APK", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Stats Grid
        item {
            Text(
                text = "STATUS SISTEM",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Kloning",
                    value = "${virtualApps.size}",
                    icon = Icons.Default.Layers,
                    color = iOSIndigo,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pengguna",
                    value = "${userList.size}",
                    icon = Icons.Default.Person,
                    color = iOSPink,
                    modifier = Modifier.weight(1f)
                )
            }
            val detectedAbi = remember {
                val abis = android.os.Build.SUPPORTED_ABIS
                if (!abis.isNullOrEmpty()) {
                    abis[0]
                } else {
                    @Suppress("DEPRECATION")
                    android.os.Build.CPU_ABI ?: "arm64-v8a"
                }
            }
            val context = LocalContext.current
            val detectedGmsStatus = remember(context) {
                try {
                    val isVirtualGmsInstalled = try {
                        top.niunaijun.blackbox.BlackBoxCore.get().isInstallGms(0)
                    } catch (e: Exception) {
                        false
                    }

                    val gmsPackageInfo = try {
                        context.packageManager.getPackageInfo("com.google.android.gms", 0)
                    } catch (e: Exception) {
                        null
                    }

                    when {
                        isVirtualGmsInstalled -> "Virtual"
                        gmsPackageInfo != null -> "Host"
                        else -> "None"
                    }
                } catch (e: Exception) {
                    "None"
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Arsitektur",
                    value = detectedAbi.uppercase(),
                    icon = Icons.Default.Storage,
                    color = iOSOrange,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "GMS Status",
                    value = detectedGmsStatus,
                    icon = Icons.Default.Shield,
                    color = iOSGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // App List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "APLIKASI TERPASANG",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                TextButton(onClick = onNavigateToVirtualSpace) {
                    Text("Lihat Semua", color = iOSBlue, fontWeight = FontWeight.Bold)
                }
            }

            if (virtualApps.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum Ada Aplikasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aplikasi yang dikloning akan muncul di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        virtualApps.take(5).forEachIndexed { index, app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLaunchApp(app) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    DrawableImage(
                                        drawable = app.icon,
                                        contentDescription = app.name,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = app.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onLaunchApp(app) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = iOSBlue
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("BUKA", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                }
                            }
                            if (index < virtualApps.take(5).size - 1) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(start = 88.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
