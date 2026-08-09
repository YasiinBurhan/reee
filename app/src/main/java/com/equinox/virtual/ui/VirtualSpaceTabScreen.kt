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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equinox.virtual.model.VirtualAppInfo

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
    Column(modifier = Modifier.fillMaxSize()) {
        // Screen Tabs: Virtual Apps / Host Apps Engine
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                text = { Text("Sandbox Virtual (${virtualApps.size})") },
                icon = { Icon(Icons.Default.Android, contentDescription = "Aplikasi Virtual") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                text = { Text("Aplikasi Perangkat (${hostApps.size})") },
                icon = { Icon(Icons.Default.DeveloperMode, contentDescription = "Aplikasi Perangkat") }
            )
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
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = "Sandbox Kosong",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sandbox Virtual Kosong",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Belum ada aplikasi yang berjalan di lingkungan virtual untuk ruang pengguna ini.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSelectHostAppsTab,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("btn_empty_clone_cta")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Pasang Aplikasi")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pasang Aplikasi dari Perangkat")
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
            placeholder = { Text("Cari aplikasi virtual...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("input_search_virtual_apps")
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunch() }
            .testTag("card_virtual_app_${app.packageName}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = "VIRTUAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hapus Data Aplikasi") },
                            leadingIcon = { Icon(Icons.Default.CleanHands, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClearData()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copot Pemasangan", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onUninstall()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App Icon
            DrawableImage(
                drawable = app.icon,
                contentDescription = app.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = app.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )

            val isWhitelisted = allowedPackages.contains(app.packageName)
            val profileLabel = if (isWhitelisted) {
                val shortName = app.name.split(" ").firstOrNull() ?: "App"
                val cleanedName = if (shortName.length > 8) shortName.take(6) + ".." else shortName
                "$cleanedName Profile"
            } else {
                null
            }
            if (profileLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = profileLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        fontSize = 8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLaunch,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = "Buka", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Buka", fontSize = 12.sp)
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
            placeholder = { Text("Cari aplikasi terpasang...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("input_search_host_apps")
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val isRegistered = allowedPackages.contains(app.packageName)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRegistered) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isRegistered) 1.0f else 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            DrawableImage(
                                drawable = app.icon,
                                contentDescription = app.name,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (!isRegistered) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(2.dp)
                                        ) {
                                            Text(
                                                text = "Belum Terdaftar",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = { onCloneApp(app.packageName) },
                            enabled = isRegistered,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Kloning", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kloning", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
