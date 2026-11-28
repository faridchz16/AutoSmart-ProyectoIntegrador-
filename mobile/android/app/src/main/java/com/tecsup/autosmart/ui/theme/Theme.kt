package com.tecsup.autosmart.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Paleta AutoSmart (misma que las pantallas del cliente): así todas las pantallas se ven igual
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6E9BFF),
    onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF1C2A4A),
    onPrimaryContainer = Color(0xFFA9C2FF),
    secondary = Color(0xFFA9C2FF),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE8EEF8),
    surface = Color(0xFF141D30),
    onSurface = Color(0xFFE8EEF8),
    surfaceVariant = Color(0xFF1C2A4A),
    onSurfaceVariant = Color(0xFF9AA8C0),
    outline = Color(0xFF26324A),
    error = Color(0xFFF87171)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1150E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9F0FF),
    onPrimaryContainer = Color(0xFF0A3AB0),
    secondary = Color(0xFF0A3AB0),
    background = Color(0xFFF5F8FE),
    onBackground = Color(0xFF0B1B3A),
    surface = Color.White,
    onSurface = Color(0xFF0B1B3A),
    surfaceVariant = Color(0xFFE9F0FF),
    onSurfaceVariant = Color(0xFF5A677F),
    outline = Color(0xFFC9D3E6),
    error = Color(0xFFDC2626)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun AutoSmartAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // colores propios de AutoSmart, no los del fondo de pantalla
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}