package com.tecsup.autosmart.ui.cliente

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.model.CitaCreadaResponse

/** Un aviso para el cliente, armado a partir del estado de sus citas. */
data class AvisoCliente(
    val clave: String,        // idCita + estado: cambia cuando la cita cambia de estado
    val titulo: String,
    val detalle: String,
    val fecha: String,
    val tipo: TipoAviso
)

enum class TipoAviso { INFO, EXITO, ALERTA, ERROR }

/** Convierte las citas del cliente en avisos (uno por cita, según su estado actual). */
fun avisosDe(citas: List<CitaCreadaResponse>): List<AvisoCliente> = citas
    .sortedWith(compareByDescending<CitaCreadaResponse> { it.fecha }.thenByDescending { it.horaInicio })
    .mapNotNull { c ->
        val hora = c.horaInicio.take(5)
        val (titulo, detalle, tipo) = when (c.estado.uppercase()) {
            "PENDIENTE" -> Triple("Cita registrada",
                "Tu cita ${c.ticket} de ${c.servicio} espera la confirmación del mecánico.", TipoAviso.INFO)
            "CONFIRMADA" -> Triple("Cita confirmada",
                "${c.mecanico} confirmó tu cita del ${c.fecha} a las $hora.", TipoAviso.EXITO)
            "RECHAZADA" -> Triple("Cita rechazada",
                "El mecánico no pudo atender tu cita ${c.ticket}. Reserva otro horario.", TipoAviso.ERROR)
            "CANCELADA" -> Triple("Cita cancelada",
                "La cita ${c.ticket} de ${c.servicio} fue cancelada.", TipoAviso.ERROR)
            "EN_PROCESO" -> Triple("Tu vehículo está en el taller",
                "Sigue el avance de ${c.servicio} (${c.placa}) en Mis citas.", TipoAviso.INFO)
            "ESPERA_APROBACION" -> Triple("Presupuesto por aprobar",
                "El mecánico envió un presupuesto para ${c.placa}. Revísalo y apruébalo.", TipoAviso.ALERTA)
            "FINALIZADA" -> Triple("¡Tu vehículo está listo!",
                "${c.servicio} de ${c.placa} terminó. Paga el servicio para recogerlo.", TipoAviso.ALERTA)
            "PAGADO" -> Triple("Pago confirmado",
                "Gracias. Descarga tu comprobante y califica a ${c.mecanico}.", TipoAviso.EXITO)
            else -> return@mapNotNull null
        }
        AvisoCliente("${c.idCita}:${c.estado.uppercase()}", titulo, detalle, "${c.fecha} · $hora", tipo)
    }

/** Guarda en el celular qué avisos ya vio cada cliente (para el número rojo de la campanita). */
object AvisosLeidos {
    private fun prefs(context: Context) = context.getSharedPreferences("avisos", Context.MODE_PRIVATE)
    fun leidos(context: Context, idUsuario: Int): Set<String> =
        prefs(context).getStringSet("leidos_$idUsuario", emptySet()).orEmpty()
    fun marcar(context: Context, idUsuario: Int, claves: Collection<String>) {
        prefs(context).edit().putStringSet("leidos_$idUsuario", leidos(context, idUsuario) + claves).apply()
    }
}

/** Campanita blanca para el encabezado azul, con el número de avisos nuevos. */
@Composable
fun CampanaAvisos(nuevos: Int, habilitado: Boolean, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick, enabled = habilitado) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones", tint = Color.White,
                modifier = Modifier.size(28.dp))
        }
        if (nuevos > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (nuevos > 9) "9+" else "$nuevos", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Panel inferior con la lista de notificaciones. Cada aviso lleva a Mis citas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelAvisos(
    avisos: List<AvisoCliente>,
    leidos: Set<String>,
    cargando: Boolean,
    onAbrir: () -> Unit,
    onCerrar: () -> Unit
) {
    val c = coloresCliente()
    ModalBottomSheet(onDismissRequest = onCerrar, containerColor = c.fondo) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, null, tint = c.azul)
                Spacer(Modifier.width(8.dp))
                Text("Notificaciones", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.texto)
            }
            Spacer(Modifier.height(12.dp))
            when {
                cargando && avisos.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.azul)
                }
                avisos.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.NotificationsNone, null, tint = c.textoSecundario, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No tienes notificaciones", fontWeight = FontWeight.Bold, color = c.texto)
                    Text("Aquí verás cuando confirmen tu cita o tu vehículo esté listo.",
                        color = c.textoSecundario, fontSize = 13.sp)
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.heightIn(max = 520.dp)
                ) {
                    items(avisos, key = { it.clave }) { aviso ->
                        FilaAviso(aviso, nuevo = aviso.clave !in leidos, onClick = onAbrir)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaAviso(aviso: AvisoCliente, nuevo: Boolean, onClick: () -> Unit) {
    val c = coloresCliente()
    val (color, icono) = estiloDe(aviso)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (nuevo) c.azulSuave else c.superficie),
        border = BorderStroke(1.dp, if (nuevo) c.azul.copy(alpha = 0.35f) else c.borde),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(icono, null, tint = color, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(aviso.titulo, fontWeight = FontWeight.Bold, color = c.texto, fontSize = 15.sp,
                        modifier = Modifier.weight(1f, fill = false))
                    if (nuevo) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                    }
                }
                Text(aviso.detalle, color = c.textoSecundario, fontSize = 13.sp, lineHeight = 17.sp)
                Text(aviso.fecha, color = c.textoSecundario.copy(alpha = 0.8f), fontSize = 11.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.textoSecundario)
        }
    }
}

private fun estiloDe(aviso: AvisoCliente): Pair<Color, ImageVector> = when {
    aviso.clave.endsWith(":FINALIZADA") -> Color(0xFF16A34A) to Icons.Outlined.TaskAlt
    aviso.clave.endsWith(":PAGADO") -> Color(0xFF16A34A) to Icons.Outlined.Payments
    aviso.clave.endsWith(":ESPERA_APROBACION") -> Color(0xFFEA580C) to Icons.Outlined.ReceiptLong
    aviso.clave.endsWith(":EN_PROCESO") -> Color(0xFF1150E8) to Icons.Outlined.Build
    aviso.tipo == TipoAviso.EXITO -> Color(0xFF16A34A) to Icons.Outlined.CheckCircle
    aviso.tipo == TipoAviso.ERROR -> Color(0xFFDC2626) to Icons.Outlined.Cancel
    else -> Color(0xFF1150E8) to Icons.Outlined.Schedule
}
