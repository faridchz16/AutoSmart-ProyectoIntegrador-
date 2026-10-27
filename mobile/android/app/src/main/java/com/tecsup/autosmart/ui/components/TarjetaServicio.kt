package com.tecsup.autosmart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.data.model.Servicio
import java.util.Locale

fun formatearPrecio(precio: Double): String = "S/ " + String.format(Locale.US, "%.2f", precio)

fun formatearDuracion(minutos: Int): String = when {
    minutos <= 0 -> "Duración por definir"
    minutos < 60 -> "$minutos min"
    minutos % 60 == 0 -> "${minutos / 60} h"
    else -> "${minutos / 60} h ${minutos % 60} min"
}

@Composable
fun ChipInfo(texto: String, destacado: Boolean = false) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (destacado) Color(0xFF60A5FA) else Color(0xFF94A3B8),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (destacado) Color(0xFF1D4ED8).copy(alpha = 0.20f) else Color(0xFF1E293B))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun TarjetaServicio(
    servicio: Servicio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), forma)
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(servicio.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (!servicio.descripcion.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = servicio.descripcion,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text("›", fontSize = 26.sp, color = Color(0xFF94A3B8))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipInfo(formatearPrecio(servicio.precioBase), destacado = true)
            ChipInfo(formatearDuracion(servicio.duracionMinutos))
        }
    }
}