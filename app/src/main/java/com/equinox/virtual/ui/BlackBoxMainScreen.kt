package com.equinox.virtual.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.equinox.virtual.model.VirtualAppInfo
import com.equinox.virtual.viewmodel.BlackBoxViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackBoxMainScreen(
    viewModel: BlackBoxViewModel,
    modifier: Modifier = Modifier
) {
    val virtualApps by viewModel.virtualApps.collectAsState()
    val hostApps by viewModel.hostApps.collectAsState()
    val userList by viewModel.userList.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val engineInitialized by viewModel.engineInitialized.collectAsState()
    val engineProgress by viewModel.engineProgress.collectAsState()
    val engineStatusText by viewModel.engineStatusText.collectAsState()
    val deviceSpoofingEnabled by viewModel.deviceSpoofingEnabled.collectAsState()
    val gmsProxyEnabled by viewModel.gmsProxyEnabled.collectAsState()
    val storageIsolationEnabled by viewModel.storageIsolationEnabled.collectAsState()
    val rootHideEnabled by viewModel.rootHideEnabled.collectAsState()
    val currentUserSession by viewModel.currentUserSession.collectAsState()
    val expiryTime by viewModel.expiryTime.collectAsState()
    val isDarkThemePref by viewModel.isDarkTheme.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isDarkTheme = isDarkThemePref ?: androidx.compose.foundation.isSystemInDarkTheme()

    val context = LocalContext.current
    var pendingLaunchApp by remember { mutableStateOf<VirtualAppInfo?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Virtual Apps, 1: Host Apps (Inside Virtual Space Tab)
    var selectedBottomNavIndex by remember { mutableIntStateOf(2) } // Default to Virtual Space (index 2)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (!Settings.canDrawOverlays(context)) {
        }
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val context = LocalContext.current
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = when (selectedBottomNavIndex) {
                                0 -> "Beranda"
                                1 -> "Pengelola Unduhan"
                                2 -> "Virtual"
                                3 -> "Profil"
                                4 -> "Pengaturan"
                                else -> "EQuinox Virtual"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            userRole?.let { role ->
                                val r = role.lowercase()
                                if (r == "admin" || r == "reseller") {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (r == "admin") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            text = role.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (r == "admin") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clickable {
                                        if (currentUserId == 0) {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("UID", com.equinox.virtual.BlackBoxApp.getDeviceHwid())
                                            clipboard.setPrimaryClip(clip)
                                            android.widget.Toast.makeText(context, "UID berhasil disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (currentUserId == 0) "UID: ${com.equinox.virtual.BlackBoxApp.getDeviceHwid()}" else "Ruang $currentUserId",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    if (currentUserId == 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Salin UID",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleTheme(isDarkTheme) },
                        modifier = Modifier.testTag("btn_toggle_theme")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (isDarkTheme) "Aktifkan Mode Terang" else "Aktifkan Mode Gelap"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(72.dp)
                    ) {
                        NavigationBarItem(
                            selected = selectedBottomNavIndex == 0,
                            onClick = { selectedBottomNavIndex = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                            label = { Text("Beranda") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_dashboard")
                        )

                        NavigationBarItem(
                            selected = selectedBottomNavIndex == 1,
                            onClick = { selectedBottomNavIndex = 1 },
                            icon = { Icon(Icons.Default.Download, contentDescription = "Unduh") },
                            label = { Text("Unduh") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_download")
                        )

                        NavigationBarItem(
                            selected = selectedBottomNavIndex == 2,
                            onClick = { selectedBottomNavIndex = 2 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (virtualApps.isNotEmpty()) {
                                            Badge { Text("${virtualApps.size}") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = "Virtual")
                                }
                            },
                            label = { Text("Virtual") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_sandbox")
                        )

                        NavigationBarItem(
                            selected = selectedBottomNavIndex == 3,
                            onClick = { selectedBottomNavIndex = 3 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                            label = { Text("Profil") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_profil")
                        )

                        val isPrivilegedUser = userRole?.lowercase() == "admin" || userRole?.lowercase() == "reseller"
                        if (isPrivilegedUser) {
                            NavigationBarItem(
                                selected = selectedBottomNavIndex == 4,
                                onClick = { selectedBottomNavIndex = 4 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Setting") },
                                label = { Text("Setting") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_setting")
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Engine Initialization Progress Banner
            BcoreEngineStatusBanner(
                engineInitialized = engineInitialized,
                engineProgress = engineProgress,
                engineStatusText = engineStatusText
            )

            AnimatedContent(
                targetState = selectedBottomNavIndex,
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                },
                label = "tab_transitions",
                modifier = Modifier.weight(1f)
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> DashboardTabScreen(
                        virtualApps = virtualApps,
                        userList = userList,
                        currentUserId = currentUserId,
                        onNavigateToVirtualSpace = { selectedBottomNavIndex = 2 },
                        onNavigateToDownload = { selectedBottomNavIndex = 1 },
                        onLaunchApp = { app ->
                            if (false) {
                                viewModel.launchVirtualApp(app.packageName, true)
                            } else {
                                pendingLaunchApp = app
                            }
                        }
                    )
                    1 -> DownloadTabScreen(
                        onInstallApkUri = { uri -> viewModel.installApkFromUri(uri) },
                        onDownloadFromUrl = { url -> viewModel.downloadAndInstallApkFromUrl(url) }
                    )
                    2 -> VirtualSpaceTabScreen(
                        userList = userList,
                        currentUserId = currentUserId,
                        virtualApps = virtualApps,
                        hostApps = hostApps,
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                        onSelectUser = { viewModel.selectUser(it) },
                        onAddUser = { viewModel.addUser() },
                        onDeleteUser = { viewModel.deleteUser(it) },
                        onLaunchVirtualApp = { app ->
                            if (false) {
                                viewModel.launchVirtualApp(app.packageName, true)
                            } else {
                                pendingLaunchApp = app
                            }
                        },
                        onClearVirtualAppData = { viewModel.clearVirtualAppData(it) },
                        onUninstallVirtualApp = { viewModel.uninstallVirtualApp(it) },
                        onCloneHostApp = { viewModel.installAppToVirtual(it) }
                    )
                    3 -> ProfilTabScreen(
                        currentUserId = currentUserId,
                        userList = userList,
                        deviceSpoofingEnabled = deviceSpoofingEnabled,
                        gmsProxyEnabled = gmsProxyEnabled,
                        storageIsolationEnabled = storageIsolationEnabled,
                        rootHideEnabled = rootHideEnabled,
                        onToggleDeviceSpoofing = { viewModel.setDeviceSpoofingEnabled(it) },
                        onToggleGmsProxy = { viewModel.setGmsProxyEnabled(it) },
                        onToggleStorageIsolation = { viewModel.setStorageIsolationEnabled(it) },
                        onToggleRootHide = { viewModel.setRootHideEnabled(it) },
                        onSelectUser = { viewModel.selectUser(it) },
                        onAddUser = { viewModel.addUser() },
                        onDeleteUser = { viewModel.deleteUser(it) },
                        onRefreshAll = { viewModel.refreshAll() },
                        onLogout = { viewModel.logout() },
                        currentUserSession = currentUserSession,
                        expiryTime = expiryTime,
                        userRole = userRole
                    )
                    4 -> SettingTabScreen(
                        userRole = userRole,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    pendingLaunchApp?.let { app ->
        LaunchModeDialog(
            app = app,
            onDismissRequest = { pendingLaunchApp = null },
            onConfirmLaunch = { isModMode ->
                viewModel.launchVirtualApp(app.packageName, isModMode)
                pendingLaunchApp = null
            }
        )
    }
}
