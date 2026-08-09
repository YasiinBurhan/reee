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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equinox.virtual.model.VirtualAppInfo
import com.equinox.virtual.ui.theme.*

@Composable
fun VirtualSpaceTabScreen(
    userList: List<Int>,
    currentUserId: Int,
    virtualApps: List<VirtualAppInfo>,
    hostApps: List<VirtualAppInfo>,
    allowedPackages: Set<String> = emptySet(),
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onSelectUser: (Int) -> Unit,
    onAddUser: () -> Unit,
    onDeleteUser: (Int) -> Unit,
    onLaunchVirtualApp: (VirtualAppInfo) -> Unit,
    onClearVirtualAppData: (String) -> Unit,
    onUninstallVirtualApp: (String) -> Unit,
    onCloneHostApp: (String) -> Unit
) {
    val tabs = listOf("SANDBOX", "PERANGKAT")

    Column(modifier = Modifier.fillMaxSize()) {
        // iOS style Segmented Control
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("SANDBOX", "PERANGKAT").forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clickable { onSelectTab(index) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                if (virtualApps.isEmpty()) {
                    EmptyVirtualAppsView(onSelectHostAppsTab = { onSelectTab(1) })
                } else {
                    VirtualAppsGrid(
                        apps = virtualApps,
                        allowedPackages = allowedPackages,
                        onLaunch = onLaunchVirtualApp,
                        onClearData = onClearVirtualAppData,
                        onUninstall = onUninstallVirtualApp
                    )
                }
            }
            1 -> {
                HostAppsList(
                    hostApps = hostApps,
                    allowedPackages = allowedPackages,
                    onCloneApp = onCloneHostApp
                )
            }
        }
    }
}

@Composable
fun EmptyVirtualAppsView(onSelectHostAppsTab: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = iOSBlue.copy(alpha = 0.1f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = iOSBlue,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Sandbox Kosong",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Belum ada aplikasi yang dikloning ke dalam sandbox ini.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSelectHostAppsTab,
            colors = ButtonDefaults.buttonColors(containerColor = iOSBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kloning Aplikasi", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VirtualAppsGrid(
    apps: List<VirtualAppInfo>,
    allowedPackages: Set<String> = emptySet(),
    onLaunch: (VirtualAppInfo) -> Unit,
    onClearData: (String) -> Unit,
    onUninstall: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari di Sandbox...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                VirtualAppCard(
                    app = app,
                    allowedPackages = allowedPackages,
                    onLaunch = { onLaunch(app) },
                    onClearData = { onClearData(app.packageName) },
                    onUninstall = { onUninstall(app.packageName) }
                )
            }
        }
    }
}

@Composable
fun VirtualAppCard(
    app: VirtualAppInfo,
    allowedPackages: Set<String> = emptySet(),
    onLaunch: () -> Unit,
    onClearData: () -> Unit,
    onUninstall: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunch() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hapus Data") },
                            leadingIcon = { Icon(Icons.Default.CleanHands, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClearData()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus APK", color = iOSRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = iOSRed) },
                            onClick = {
                                menuExpanded = false
                                onUninstall()
                            }
                        )
                    }
                }
            }

            DrawableImage(
                drawable = app.icon,
                contentDescription = app.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = iOSBlue),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("BUKA", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HostAppsList(
    hostApps: List<VirtualAppInfo>,
    allowedPackages: Set<String> = emptySet(),
    onCloneApp: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(hostApps, searchQuery) {
        if (searchQuery.isBlank()) hostApps
        else hostApps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari aplikasi di perangkat...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isWhitelisted = allowedPackages.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isWhitelisted) { onCloneApp(app.packageName) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .alpha(if (isWhitelisted) 1.0f else 0.5f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            DrawableImage(
                                drawable = app.icon,
                                contentDescription = app.name,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isWhitelisted) app.packageName else "BELUM TERDAFTAR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isWhitelisted) MaterialTheme.colorScheme.onSurfaceVariant else iOSRed,
                                    fontWeight = if (isWhitelisted) FontWeight.Normal else FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = { onCloneApp(app.packageName) },
                            enabled = isWhitelisted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = iOSBlue,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("KLON", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
