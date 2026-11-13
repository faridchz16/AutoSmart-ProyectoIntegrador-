package com.tecsup.autosmart.ui.auth

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Estilo visual de Bienvenida, Inicio de sesión y Registro (Variante 4B de Figma).
 * Fondo azul en diagonal con brillo celeste arriba, como el panel de la web.
 */
object AuthEstilo {
    val AzulOscuro = Color(0xFF0A3AB0)
    val AzulWeb = Color(0xFF1150E8)
    val Marino = Color(0xFF0B1B3A)
    val Celeste = Color(0xFF22D3EE)
    val TextoSuave = Color(0xFFCFD9EF)
    val Error = Color(0xFFFECACA)
    val Blanco = Color.White
}

/** Fondo degradado azul + brillo celeste en la parte superior. */
fun Modifier.fondoAuth(): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.linearGradient(
            0f to AuthEstilo.AzulOscuro,
            0.55f to AuthEstilo.AzulWeb,
            1f to AuthEstilo.Marino,
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(AuthEstilo.Celeste.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(size.width / 2f, 0f),
            radius = size.width * 0.9f
        )
    )
}

/**
 * Menú de texto del sistema sin las opciones Copiar y Cortar.
 * Pegar y Seleccionar todo siguen funcionando (contraseñas guardadas, autocompletar).
 */
private class BarraSinCopiar(private val base: TextToolbar) : TextToolbar {
    override val status: TextToolbarStatus get() = base.status
    override fun hide() = base.hide()
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) = base.showMenu(rect, null, onPasteRequested, null, onSelectAllRequested)
}

/** Envuelve un campo de contraseña para que su texto no se pueda copiar ni cortar. */
@Composable
fun SinCopiar(content: @Composable () -> Unit) {
    val base = LocalTextToolbar.current
    val barra = remember(base) { BarraSinCopiar(base) }
    CompositionLocalProvider(LocalTextToolbar provides barra, content = content)
}

/** Campos de texto translúcidos sobre el fondo azul. */
@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.16f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.10f),
    errorContainerColor = Color.White.copy(alpha = 0.10f),
    disabledContainerColor = Color.White.copy(alpha = 0.06f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    errorTextColor = Color.White,
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
    errorBorderColor = AuthEstilo.Error,
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.85f),
    errorLabelColor = AuthEstilo.Error,
    focusedSupportingTextColor = AuthEstilo.TextoSuave,
    unfocusedSupportingTextColor = AuthEstilo.TextoSuave,
    errorSupportingTextColor = AuthEstilo.Error,
    cursorColor = Color.White,
    errorCursorColor = Color.White
)
