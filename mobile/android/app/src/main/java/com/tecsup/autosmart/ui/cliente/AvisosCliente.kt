package com.tecsup.autosmart.ui.cliente

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.data.repository.CitaRepository
import com.tecsup.autosmart.data.repository.PerfilRepository
import com.tecsup.autosmart.model.CitaCreadaResponse
import com.tecsup.autosmart.ui.components.BarraAutoSmart

/** Un aviso para el cliente, armado a partir del estado de sus citas. */
data class AvisoCliente(
    val clave: String,        // idCita + estado: cambia cuando la cita cambia de estado
    val titulo: String,
    val detalle: String,
    val fecha: String,
    val tipo: TipoAviso,
    val idCita: Long = 0L,
    val placa: String = "",
    val estado: String = ""
)

enum class TipoAviso { INFO, EXITO, ALERTA, ERROR }

/** Convierte las citas del cliente en avisos (uno por cita, según su estado actual). */
fun avisosDe(citas: List<CitaCreadaResponse>): List<AvisoCliente> = citas
    .sortedWith(compareByDescending<CitaCreadaResponse> { it.fecha }.thenByDescending { it.horaInicio })
    .mapNotNull { c ->
        val hora = c.horaInicio.take(5)
        val estado = c.estado.uppercase()
        val (titulo, detalle, tipo) = when (estado) {
            "PENDIENTE" -> Triple("Cita registrada",
                "Tu cita ${c.ticket} de ${c.servicio} espera la confirmación del mecánico.", TipoAviso.INFO)
            "CONFIRMADA" -> Triple("Cita confirmada",
                "${c.mecanico} confirmó tu cita del ${c.fecha} a las $hora.", TipoAviso.EXITO)
            "RECHAZADA" -> Triple("Cita rechazada",
                "El mecánico no pudo atender tu cita ${c.ticket}. Reserva otro horario.", TipoAviso.ERROR)
            "CANCELADA" -> Triple("Cita cancelada",
                "La cita ${c.ticket} de ${c.servicio} fue cancelada.", TipoAviso.ERROR)
            "EN_PROCESO" -> Triple("Tu vehículo está en el taller",
                "Toca para ver el avance de ${c.servicio} (${c.placa}).", TipoAviso.INFO)
            "ESPERA_APROBACION" -> Triple("Presupuesto por aprobar",
                "El mecánico envió un presupuesto para ${c.placa}. Toca para revisarlo.", TipoAviso.ALERTA)
            "FINALIZADA" -> Triple("¡Tu vehículo está listo!",
                "${c.servicio} de ${c.placa} terminó. Toca para pagar el servicio.", TipoAviso.ALERTA)
            "PAGADO" -> Triple("Pago confirmado",
                "Gracias. Descarga tu comprobante y califica a ${c.mecanico}.", TipoAviso.EXITO)
            else -> return@mapNotNull null
        }
        AvisoCliente("${c.idCita}:$estado", titulo, detalle, "${c.fecha} · $hora", tipo, c.idCita, c.placa, estado)
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

/**
 * Pantalla de notificaciones del cliente: avisos nuevos arriba y anteriores abajo.
 * Al tocar un aviso se marca como leído y [onAbrir] lleva a su pantalla (pago, presupuesto, seguimiento o Mis citas).
 */
@Composable
fun NotificacionesScreen(onBack: () -> Unit, onAbrir: (AvisoCliente) -> Unit) {
    val contexto = LocalContext.current
    val c = coloresCliente()
    var idUsuario by remember { mutableStateOf<Int?>(null) }
    var avisos by remember { mutableStateOf<List<AvisoCliente>>(emptyList()) }
    var leidos by remember { mutableStateOf<Set<String>>(emptySet()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var recargar by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onBack)
    LaunchedEffect(recargar) {
        cargando = true
        error = null
        val id = idUsuario ?: PerfilRepository().obtenerPerfil().getOrNull()?.idUsuario
        idUsuario = id
        if (id != null) leidos = AvisosLeidos.leidos(contexto, id)
        CitaRepository().misCitas()
            .onSuccess { avisos = avisosDe(it) }
            .onFailure { error = it.message ?: "No se pudieron cargar las notificaciones. Revisa tu conexión." }
        cargando = false
    }
    val marcar: (Collection<String>) -> Unit = { claves ->
        idUsuario?.let { id ->
            AvisosLeidos.marcar(contexto, id, claves)
            leidos = AvisosLeidos.leidos(contexto, id)
        }
    }
    val nuevos = avisos.filter { it.clave !in leidos }
    val anteriores = avisos.filter { it.clave in leidos }

    Scaffold(
        containerColor = c.fondo,
        topBar = {
            BarraAutoSmart(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (nuevos.isNotEmpty()) {
                        IconButton(onClick = { marcar(nuevos.map { it.clave }) }) {
                            Icon(Icons.Outlined.DoneAll, contentDescription = "Marcar todas como leídas")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                cargando && avisos.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = c.azul)
                error != null && avisos.isEmpty() -> Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error.orEmpty(), color = c.textoSecundario)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { recargar++ }, colors = ButtonDefaults.buttonColors(containerColor = c.azul)) {
                        Text("Reintentar")
                    }
                }
                avisos.isEmpty() -> Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(c.azulSuave),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.NotificationsNone, null, tint = c.azul, modifier = Modifier.size(36.dp)) }
                    Spacer(Modifier.height(12.dp))
                    Text("No tienes notificaciones", fontWeight = FontWeight.Bold, color = c.texto, fontSize = 17.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Aquí verás cuando confirmen tu cita o tu vehículo esté listo.",
                        color = c.textoSecundario, fontSize = 13.sp)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (nuevos.isNotEmpty()) {
                        item(key = "t_nuevas") { TituloGrupo("Nuevas", nuevos.size) }
                        items(nuevos, key = { it.clave }) { aviso ->
                            FilaAviso(aviso, nuevo = true) { marcar(listOf(aviso.clave)); onAbrir(aviso) }
                        }
                    }
                    if (anteriores.isNotEmpty()) {
                        item(key = "t_anteriores") { TituloGrupo("Anteriores", null) }
                        items(anteriores, key = { it.clave }) { aviso ->
                            FilaAviso(aviso, nuevo = false) { onAbrir(aviso) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TituloGrupo(texto: String, cantidad: Int?) {
    val c = coloresCliente()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) {
        Text(texto, fontWeight = FontWeight.ExtraBold, color = c.texto, fontSize = 16.sp)
        if (cantidad != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFDC2626)).padding(horizontal = 8.dp, vertical = 1.dp)
            ) { Text("$cantidad", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
                Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)),
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
    aviso.estado == "FINALIZADA" -> Color(0xFF16A34A) to Icons.Outlined.TaskAlt
    aviso.estado == "PAGADO" -> Color(0xFF16A34A) to Icons.Outlined.Payments
    aviso.estado == "ESPERA_APROBACION" -> Color(0xFFEA580C) to Icons.Outlined.ReceiptLong
    aviso.estado == "EN_PROCESO" -> Color(0xFF1150E8) to Icons.Outlined.Build
    aviso.tipo == TipoAviso.EXITO -> Color(0xFF16A34A) to Icons.Outlined.CheckCircle
    aviso.tipo == TipoAviso.ERROR -> Color(0xFFDC2626) to Icons.Outlined.Cancel
    else -> Color(0xFF1150E8) to Icons.Outlined.Schedule
}
