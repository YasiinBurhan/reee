package com.equinox.virtual.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// iOS Style Color Palette
private val IosDarkBg = Color(0xEC1C1C1E)
private val IosCardBg = Color(0x28767680)
private val IosHeaderBg = Color(0xF22C2C2E)
private val IosBorder = Color(0x388E8E93)
private val IosAccentBlue = Color(0xFF0A84FF)
private val IosAccentGreen = Color(0xFF30D158)
private val IosAccentPurple = Color(0xFFBF5AF2)
private val IosTextPrimary = Color(0xFFFFFFFF)
private val IosTextSecondary = Color(0x8AFFFFFF)
private val IosSegmentBg = Color(0x3D767680)

@Composable
fun MenuModHUD(
    enabled: Boolean,
    userRole: String?,
    onClose: () -> Unit
) {
    if (!enabled) return

    var offsetX by remember { mutableFloatStateOf(30f) }
    var offsetY by remember { mutableFloatStateOf(140f) }
    var isExpanded by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // MenuMod Mod States
    var watermarkEnabled by remember { mutableStateOf(true) }
    var touchIntercept by remember { mutableStateOf(true) }
    var fpsBoostEnabled by remember { mutableStateOf(true) }
    var espOverlayEnabled by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var surfaceAlpha by remember { mutableFloatStateOf(0.92f) }
    var mockFps by remember { mutableIntStateOf(60) }

    // Simulating active FPS count
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            mockFps = (58..60).random()
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val menuWidthDp = if (isExpanded) 310.dp else 140.dp
        val menuHeightDp = if (isExpanded) 320.dp else 46.dp
        val maxAvailableWidthPx = constraints.maxWidth.toFloat()
        val maxAvailableHeightPx = constraints.maxHeight.toFloat()
        
        val menuWidthPx = with(density) { menuWidthDp.toPx() }
        val menuHeightPx = with(density) { menuHeightDp.toPx() }

        val maxX = (maxAvailableWidthPx - menuWidthPx).coerceAtLeast(0f)
        val maxY = (maxAvailableHeightPx - menuHeightPx).coerceAtLeast(0f)

        // Ensure current offset stays inside screen boundaries
        val currentX = offsetX.coerceIn(0f, maxX)
        val currentY = offsetY.coerceIn(0f, maxY)

        Box(
            modifier = Modifier
                .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                .pointerInput(maxX, maxY) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxX)
                        offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxY)
                    }
                }
        ) {
            if (!isExpanded) {
                // Collapsed Dynamic Island / iOS Capsule Floating Pill
                Surface(
                    onClick = { isExpanded = true },
                    shape = RoundedCornerShape(24.dp),
                    color = IosDarkBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, IosBorder),
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IosAccentGreen)
                        )
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = IosAccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "MenuMod",
                            color = IosTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = IosAccentGreen.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "$mockFps FPS",
                                color = IosAccentGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else {
                // Expanded iOS Glassmorphism Card Overlay
                Surface(
                    modifier = Modifier
                        .width(310.dp)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(22.dp),
                    color = IosDarkBg.copy(alpha = surfaceAlpha),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IosBorder),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        // iOS Drag Capsule Handle + Title Header Bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(IosHeaderBg)
                                .padding(top = 8.dp, bottom = 10.dp, start = 14.dp, end = 10.dp)
                        ) {
                            // Top Drag Handle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(IosAccentBlue, IosAccentPurple)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "MenuMod iOS HUD",
                                            color = IosTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                        Text(
                                            text = "ByteHook Active • $mockFps FPS",
                                            color = IosAccentGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { isExpanded = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Minimize",
                                            tint = IosTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onClose,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = IosTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // iOS Segmented Control Tabs
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = IosSegmentBg
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val tabs = listOf("Features", "Engine", "Logs")
                                tabs.forEachIndexed { index, title ->
                                    val isSelected = selectedTab == index
                                    Surface(
                                        onClick = { selectedTab = index },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp),
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (isSelected) IosHeaderBg else Color.Transparent,
                                        shadowElevation = if (isSelected) 2.dp else 0.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = title,
                                                color = if (isSelected) IosTextPrimary else IosTextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Tab Contents Container
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (selectedTab) {
                                0 -> { // Features Tab
                                    IosToggleRow(
                                        icon = Icons.Default.Visibility,
                                        title = "Watermark Overlay",
                                        subtitle = "Tampilkan watermark MenuMod",
                                        checked = watermarkEnabled,
                                        onCheckedChange = { watermarkEnabled = it }
                                    )

                                    IosToggleRow(
                                        icon = Icons.Default.FlashOn,
                                        title = "FPS Boost Mode",
                                        subtitle = "Optimalkan render frame rate",
                                        checked = fpsBoostEnabled,
                                        onCheckedChange = { fpsBoostEnabled = it }
                                    )

                                    IosToggleRow(
                                        icon = Icons.Default.Security,
                                        title = "Touch Intercept",
                                        subtitle = "Tangkap input layar sentuh",
                                        checked = touchIntercept,
                                        onCheckedChange = { touchIntercept = it }
                                    )

                                    IosToggleRow(
                                        icon = Icons.Default.BugReport,
                                        title = "ESP Overlay Mode",
                                        subtitle = "Visual frame bantuan diatas layar",
                                        checked = espOverlayEnabled,
                                        onCheckedChange = { espOverlayEnabled = it }
                                    )

                                    // Speed Multiplier Slider
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = IosCardBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Speed Multiplier", color = IosTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                Text("${String.format("%.1f", speedMultiplier)}x", color = IosAccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Slider(
                                                value = speedMultiplier,
                                                onValueChange = { speedMultiplier = it },
                                                valueRange = 0.5f..3.0f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = IosAccentBlue,
                                                    activeTrackColor = IosAccentBlue,
                                                    inactiveTrackColor = IosBorder
                                                )
                                            )
                                        }
                                    }

                                    // Transparency Slider
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = IosCardBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Menu Transparency", color = IosTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                Text("${(surfaceAlpha * 100).roundToInt()}%", color = IosAccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Slider(
                                                value = surfaceAlpha,
                                                onValueChange = { surfaceAlpha = it },
                                                valueRange = 0.4f..1.0f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = IosAccentBlue,
                                                    activeTrackColor = IosAccentBlue,
                                                    inactiveTrackColor = IosBorder
                                                )
                                            )
                                        }
                                    }
                                }
                                1 -> { // Engine Tab
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = IosCardBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("PLT SYSTEM HOOK STATUS", color = IosAccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            HorizontalDivider(color = IosBorder)
                                            IosStatusBullet("eglSwapBuffers", "HOOKED (ByteHook)", true)
                                            IosStatusBullet("AInputQueue_getEvent", "HOOKED (Active)", true)
                                            IosStatusBullet("__system_property_get", "RESOLVED", true)
                                            IosStatusBullet("Direct Surface Canvas", "ACTIVE", true)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = IosCardBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("ENGINE CONFIGURATION", color = IosAccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Mode: ByteHook Automatic PLT", color = IosTextSecondary, fontSize = 10.sp)
                                            Text("Render Engine: ImGui v1.89 + Jetpack Compose", color = IosTextSecondary, fontSize = 10.sp)
                                            Text("Isolation Container: Active", color = IosTextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                                2 -> { // Logs Tab
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = IosCardBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("[ByteHook Engine] Initialization complete", color = IosAccentGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("[NativeCore] MenuMod overlay attached to surface", color = IosAccentBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("[EGL] eglSwapBuffers hook running", color = IosTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("[Touch] Input dispatch hook initialized", color = IosTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IosToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = IosCardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (checked) IosAccentBlue.copy(alpha = 0.2f) else IosBorder.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) IosAccentBlue else IosTextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        color = IosTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        color = IosTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = IosAccentGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = IosBorder
                )
            )
        }
    }
}

@Composable
private fun IosStatusBullet(
    title: String,
    statusMsg: String,
    isOk: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = IosTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(
            statusMsg,
            color = if (isOk) IosAccentGreen else Color.Red,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
