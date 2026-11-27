package com.tecsup.autosmart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Degradado azul de AutoSmart (el mismo del inicio del cliente). */
val DegradadoAutoSmart = Brush.linearGradient(listOf(Color(0xFF0A3AB0), Color(0xFF1150E8), Color(0xFF3B82F6)))

/**
 * Encabezado único de la app: azul con esquinas inferiores curvas y contenido en blanco.
 * Usa los mismos parámetros que TopAppBar (title, navigationIcon, actions) para reemplazarlo directamente.
 */
@Composable
fun BarraAutoSmart(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    // Dentro del encabezado, botones de texto e íconos se ven en blanco
    val esquema = MaterialTheme.colorScheme.copy(
        primary = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color.White.copy(alpha = 0.85f)
    )
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(DegradadoAutoSmart)
        // Sin statusBarsPadding: MainActivity ya deja el espacio de la barra de estado
    ) {
        MaterialTheme(colorScheme = esquema) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navigationIcon()
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.weight(1f).padding(start = 4.dp)) {
                        ProvideTextStyle(TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)) {
                            title()
                        }
                    }
                    actions()
                }
            }
        }
    }
}
