package com.example.cantina.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VinacciaPrimary,
    secondary = VinacciaSecondary,
    tertiary = VinacciaTertiary,
    background = Color(0xFF1A1112),
    surface = Color(0xFF1A1112),
    onPrimary = Color(0xFF561E25),
    onSecondary = Color(0xFF44292C),
    onTertiary = Color(0xFF462A08),
    onBackground = Color(0xFFF1DEDE),
    onSurface = Color(0xFFF1DEDE),
    primaryContainer = Color(0xFF752F37),
    secondaryContainer = Color(0xFF5D3F42),
    surfaceVariant = Color(0xFF524344)
)

private val LightColorScheme = lightColorScheme(
    primary = VinacciaPrimary,
    onPrimary = VinacciaOnPrimary,
    primaryContainer = VinacciaPrimaryContainer,
    onPrimaryContainer = VinacciaOnPrimaryContainer,
    secondary = VinacciaSecondary,
    onSecondary = VinacciaOnSecondary,
    secondaryContainer = VinacciaSecondaryContainer,
    onSecondaryContainer = VinacciaOnSecondaryContainer,
    tertiary = VinacciaTertiary,
    onTertiary = VinacciaOnTertiary,
    tertiaryContainer = VinacciaTertiaryContainer,
    onTertiaryContainer = VinacciaOnTertiaryContainer,
    error = VinacciaError,
    onError = VinacciaOnError,
    errorContainer = VinacciaErrorContainer,
    onErrorContainer = VinacciaOnErrorContainer,
    background = VinacciaBackground,
    onBackground = VinacciaOnBackground,
    surface = VinacciaSurface,
    onSurface = VinacciaOnSurface,
    surfaceVariant = VinacciaSurfaceVariant,
    onSurfaceVariant = VinacciaOnSurfaceVariant,
    outline = VinacciaOutline
)

@Composable
fun CantinaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Impostiamo false per usare i nostri colori vinaccia
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
