package com.akshayasahayi.companion.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. ---> YOUR EXACT LOGO COLORS <---
val AkshayaNavy = Color(0xFF0A2240)
val AkshayaTeal = Color(0xFF00A08A)

// 2. ---> LOCK IN THE LIGHT THEME PALETTE <---
private val LightColorScheme = lightColorScheme(
    primary = AkshayaNavy,
    onPrimary = Color.White,
    secondary = AkshayaTeal,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = Color(0xFFDC2626)
)

@Composable
fun AkshayaSahayiTheme(
    // 3. ---> FORCE DYNAMIC COLOR OFF & LIGHT MODE ON <---
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Ensure you have your Typography imported/defined
        content = content
    )
}