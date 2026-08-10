package com.equinox.virtual.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Custom ImGui Dark Theme Palette
private val ImGuiBg = Color(0xF2141419)
private val ImGuiHeader = Color(0xFF2A2B36)
private val ImGuiBorder = Color(0xFF3F4154)
private val ImGuiAccent = Color(0xFF00E5FF)
private val ImGuiText = Color(0xFFE0E6ED)
private val ImGuiTextMuted = Color(0xFF8C96A5)
private val ImGuiGreen = Color(0xFF00E676)

@Composable
fun ImGuiOverlayHUD(
    enabled: Boolean,
    userRole: String?,
    onClose: () -> Unit
) {
    val isAdmin = userRole?.lowercase() == "admin"
    if (!enabled || !isAdmin) return

    var offsetX by remember { mutableFloatStateOf(40f) }
    var offsetY by remember { mutableFloatStateOf(160f) }
    var isExpanded by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // ImGui Mock State Controls
    var watermarkEnabled by remember { mutableStateOf(true) }
    var touchIntercept by remember { mutableStateOf(true) }
    var surfaceAlpha by remember { mutableFloatStateOf(0.95f) }
    var mockFps by remember { mutableIntStateOf(60) }

    // Simulating active FPS count
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            mockFps = (58..60).random()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceAtLeast(0f)
                        offsetY = (offsetY + dragAmount.y).coerceAtLeast(0f)
                    }
                }
        ) {
            if (!isExpanded) {
                // Collapsed Floating ImGui Badge
                Surface(
                    onClick = { isExpanded = true },
                    shape = RoundedCornerShape(20.dp),
                    color = ImGuiBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImGuiAccent),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ImGuiGreen)
                        )
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = ImGuiAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ImGui HUD",
                            color = ImGuiText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                // Expanded ImGui Window Overlay
                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(8.dp),
                    color = ImGuiBg.copy(alpha = surfaceAlpha),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImGuiBorder),
                    shadowElevation = 12.dp
                ) {
                    Column {
                        // Title Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ImGuiHeader)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ImGuiGreen)
                                )
                                Text(
                                    text = "ImGui v1.89 (ByteHook Admin)",
                                    color = ImGuiAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { isExpanded = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Minimize,
                                        contentDescription = "Minimize",
                                        tint = ImGuiTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = ImGuiTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Tab Header Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ImGuiHeader.copy(alpha = 0.5f))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val tabs = listOf("Main", "ByteHook", "Logs")
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                TextButton(
                                    onClick = { selectedTab = index },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (isSelected) ImGuiAccent else ImGuiTextMuted
                                    )
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Tab Content
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (selectedTab) {
                                0 -> { // Main Tab
                                    Text("Mode: Admin Direct Canvas Overlay", color = ImGuiAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("Status: Running ($mockFps FPS)", color = ImGuiGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("Target: Direct Virtual Space Window", color = ImGuiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                                    HorizontalDivider(color = ImGuiBorder, modifier = Modifier.padding(vertical = 4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Draw Watermark", color = ImGuiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Checkbox(
                                            checked = watermarkEnabled,
                                            onCheckedChange = { watermarkEnabled = it },
                                            colors = CheckboxDefaults.colors(checkedColor = ImGuiAccent)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Touch Intercept", color = ImGuiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Checkbox(
                                            checked = touchIntercept,
                                            onCheckedChange = { touchIntercept = it },
                                            colors = CheckboxDefaults.colors(checkedColor = ImGuiAccent)
                                        )
                                    }

                                    Column {
                                        Text("Surface Alpha: ${(surfaceAlpha * 100).roundToInt()}%", color = ImGuiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Slider(
                                            value = surfaceAlpha,
                                            onValueChange = { surfaceAlpha = it },
                                            valueRange = 0.4f..1.0f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = ImGuiAccent,
                                                activeTrackColor = ImGuiAccent
                                            )
                                        )
                                    }
                                }
                                1 -> { // ByteHook Tab
                                    Text("[PLT HOOK STATS]", color = ImGuiAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text("• eglSwapBuffers: HOOKED (bytehook_hook_all)", color = ImGuiText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("• AInputQueue_getEvent: HOOKED", color = ImGuiText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("• __system_property_get: HOOKED", color = ImGuiText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("ByteHook Engine Mode: AUTOMATIC", color = ImGuiTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("Direct Surface Hooking: ACTIVE", color = ImGuiGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                2 -> { // Logs Tab
                                    Text("[ByteHook Engine] Hook initialization complete", color = ImGuiGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("[NativeCore] ImGui surface overlay active", color = ImGuiAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("[EGL] eglSwapBuffers hook running", color = ImGuiTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
