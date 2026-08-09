package com.equinox.virtual.ui.theme

import android.os.Build
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = iOSBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = iOSIndigo,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
    secondary = iOSPink,
    tertiary = iOSPurple,
    background = iOSDarkBackground,
    surface = iOSDarkSurface,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2C2C2E),
    onSurfaceVariant = iOSSystemGray4
)

private val LightColorScheme = lightColorScheme(
    primary = iOSBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = iOSTeal.copy(alpha = 0.1f),
    onPrimaryContainer = iOSBlue,
    secondary = iOSPink,
    tertiary = iOSPurple,
    background = iOSBackground,
    surface = iOSSurface,
    onBackground = androidx.compose.ui.graphics.Color.Black,
    onSurface = androidx.compose.ui.graphics.Color.Black,
    surfaceVariant = iOSSystemGray6,
    onSurfaceVariant = iOSSystemGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to maintain iOS aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = android.graphics.Color.TRANSPARENT
      window.navigationBarColor = android.graphics.Color.TRANSPARENT
      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = !darkTheme
      insetsController.isAppearanceLightNavigationBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
