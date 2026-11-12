package com.tecsup.autosmart.ui.auth

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

/** Menú de texto vacío: no aparecen Copiar, Cortar, Pegar ni Seleccionar todo. */
private object BarraTextoVacia : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden
    override fun hide() {}
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {}
}

/** Envuelve un campo de contraseña para que no se pueda copiar ni pegar desde el menú. */
@Composable
fun SinCopiarPegar(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextToolbar provides BarraTextoVacia, content = content)
}

/**
 * Acepta escribir un carácter a la vez o borrar, pero rechaza texto pegado
 * (por ejemplo, desde el portapapeles del teclado).
 */
fun sinPegar(anterior: String, nuevo: String): String {
    val prefijo = anterior.commonPrefixWith(nuevo).length
    val sufijo = anterior.substring(prefijo).commonSuffixWith(nuevo.substring(prefijo)).length
    val insertados = nuevo.length - prefijo - sufijo
    return if (insertados <= 1) nuevo else anterior
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
