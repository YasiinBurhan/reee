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

import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars

import com.equinox.virtual.ui.theme.*

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
    val allowedPackages by viewModel.allowedPackages.collectAsState()
    val isDarkTheme = isDarkThemePref ?: androidx.compose.foundation.isSystemInDarkTheme()

    val context = LocalContext.current
    var pendingLaunchApp by remember { mutableStateOf<VirtualAppInfo?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } 
    var selectedBottomNavIndex by remember { mutableIntStateOf(2) }

    // Sub-screens state for settings
    var activeSettingsSubScreen by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (activeSettingsSubScreen == null) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (selectedBottomNavIndex) {
                                0 -> "Beranda"
                                1 -> "Unduh"
                                2 -> "Virtual"
                                3 -> "Profil"
                                4 -> "Pengaturan"
                                else -> "EQuinox"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleTheme(isDarkTheme) }
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        },
        bottomBar = {
            if (activeSettingsSubScreen == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 20.dp)
                        .navigationBarsPadding()
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 8.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            modifier = Modifier.height(80.dp)
                        ) {
                            val navItems = listOf(
                                Triple(0, Icons.Default.Home, "Beranda"),
                                Triple(1, Icons.Default.Download, "Unduh"),
                                Triple(2, Icons.Default.Layers, "Virtual"),
                                Triple(3, Icons.Default.Person, "Profil")
                            )

                            navItems.forEach { (index, icon, label) ->
                                NavigationBarItem(
                                    selected = selectedBottomNavIndex == index,
                                    onClick = { selectedBottomNavIndex = index },
                                    icon = {
                                        if (index == 2) {
                                            BadgedBox(
                                                badge = {
                                                    if (virtualApps.isNotEmpty()) {
                                                        Badge(containerColor = iOSRed) { Text("${virtualApps.size}") }
                                                    }
                                                }
                                            ) {
                                                Icon(icon, contentDescription = label)
                                            }
                                        } else {
                                            Icon(icon, contentDescription = label)
                                        }
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = Color.Transparent,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            val isPrivilegedUser = userRole?.lowercase() == "admin" || userRole?.lowercase() == "reseller"
                            if (isPrivilegedUser) {
                                NavigationBarItem(
                                    selected = selectedBottomNavIndex == 4,
                                    onClick = { selectedBottomNavIndex = 4 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Setting") },
                                    label = { Text("Setting", style = MaterialTheme.typography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = Color.Transparent,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        val contentPadding = if (activeSettingsSubScreen != null) PaddingValues(0.dp) else paddingValues
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // Engine Initialization Progress Banner
            if (!engineInitialized || engineProgress < 1.0f) {
                BcoreEngineStatusBanner(
                    engineInitialized = engineInitialized,
                    engineProgress = engineProgress,
                    engineStatusText = engineStatusText
                )
            }

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
                        allowedPackages = allowedPackages,
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
                    4 -> {
                        when (activeSettingsSubScreen) {
                            "user_management" -> {
                                val users by viewModel.firestoreUsers.collectAsState()
                                val isLoadingUser by viewModel.isLoading.collectAsState()
                                LaunchedEffect(Unit) { viewModel.fetchFirestoreUsers() }
                                UserManagementScreen(
                                    users = users,
                                    isLoading = isLoadingUser,
                                    currentUserRole = userRole,
                                    onBack = { activeSettingsSubScreen = null },
                                    onUpdateUser = { uid, updates -> viewModel.updateFirestoreUser(uid, updates) },
                                    onDeleteUser = { uid -> viewModel.deleteFirestoreUser(uid) }
                                )
                            }
                            "allowed_packages" -> {
                                val packages by viewModel.allowedPackageList.collectAsState()
                                val isLoadingPackages by viewModel.isLoading.collectAsState()
                                AllowedPackagesManagementScreen(
                                    packages = packages,
                                    isLoading = isLoadingPackages,
                                    onBack = { activeSettingsSubScreen = null },
                                    onAddPackage = { pkg, name -> viewModel.addAllowedPackage(pkg, name) },
                                    onDeletePackage = { pkg -> viewModel.deleteAllowedPackage(pkg) }
                                )
                            }
                            "system_stats" -> {
                                val stats by viewModel.systemStats.collectAsState()
                                val isLoadingStats by viewModel.isLoading.collectAsState()
                                LaunchedEffect(Unit) { viewModel.fetchSystemStats() }
                                SystemStatisticsScreen(
                                    stats = stats,
                                    isLoading = isLoadingStats,
                                    onBack = { activeSettingsSubScreen = null }
                                )
                            }
                            else -> {
                                SettingTabScreen(
                                    userRole = userRole,
                                    viewModel = viewModel,
                                    onNavigateToUserManagement = { activeSettingsSubScreen = "user_management" },
                                    onNavigateToAllowedPackages = { activeSettingsSubScreen = "allowed_packages" },
                                    onNavigateToSystemStats = { activeSettingsSubScreen = "system_stats" }
                                )
                            }
                        }
                    }
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
