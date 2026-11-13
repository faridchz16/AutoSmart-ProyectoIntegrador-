package com.tecsup.autosmart.ui.cliente

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Paleta de las pantallas del cliente en modo claro y oscuro. */
class ColoresCliente(
    val fondo: Color,
    val superficie: Color,
    val borde: Color,
    val texto: Color,
    val textoSecundario: Color,
    val azul: Color,
    val azulOscuro: Color,
    val azulSuave: Color,
    val rojo: Color,
    val rojoSuave: Color
)

private val Claro = ColoresCliente(
    fondo = Color(0xFFF5F8FE),
    superficie = Color.White,
    borde = Color(0xFFE3E9F4),
    texto = Color(0xFF0B1B3A),
    textoSecundario = Color(0xFF5A677F),
    azul = Color(0xFF1150E8),
    azulOscuro = Color(0xFF0A3AB0),
    azulSuave = Color(0xFFE9F0FF),
    rojo = Color(0xFFDC2626),
    rojoSuave = Color(0xFFFEF2F2)
)

private val Oscuro = ColoresCliente(
    fondo = Color(0xFF0B1220),
    superficie = Color(0xFF141D30),
    borde = Color(0xFF26324A),
    texto = Color(0xFFE8EEF8),
    textoSecundario = Color(0xFF9AA8C0),
    azul = Color(0xFF6E9BFF),
    azulOscuro = Color(0xFFA9C2FF),
    azulSuave = Color(0xFF1C2A4A),
    rojo = Color(0xFFF87171),
    rojoSuave = Color(0xFF3A1A20)
)

/** Elige la paleta según el tema activo de la app. */
@Composable
fun coloresCliente(): ColoresCliente =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Oscuro else Claro
