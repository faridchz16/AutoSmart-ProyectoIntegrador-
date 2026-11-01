package com.tecsup.autosmart.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.autosmart.ui.viewmodel.MisCitasViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CitaUI(
    val id: Long,
    val servicio: String,
    val vehiculo: String,
    val mecanico: String,
    val fecha: String,
    val hora: String,
    val estado: String,
    val ticket: String = "",
    val horaFin: String = "",
    val precio: BigDecimal? = null
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisCitasScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: MisCitasViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val cancelacion by viewModel.cancelacion.collectAsState()
    val cancelando = cancelacion.enviando || cancelacion.verificando
    BackHandler(enabled = cancelacion.cita != null) {
        if (!cancelando) viewModel.cerrarCancelacion()
    }
    val contexto = LocalContext.current
    val actividad = remember(contexto) {
        generateSequence(contexto) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
    }
    LaunchedEffect(viewModel) { viewModel.cargarSiNecesario() }
    DisposableEffect(viewModel, actividad) {
        onDispose {
            if (actividad?.isChangingConfigurations != true) viewModel.limpiar()
        }
    }
    val listaCitas = remember(estado.citas) {
        estado.citas.map { cita ->
            CitaUI(
                id = cita.idCita,
                servicio = cita.servicio,
                vehiculo = "Placa: ${cita.placa}",
                mecanico = cita.mecanico,
                fecha = cita.fecha,
                hora = cita.horaInicio.take(5),
                estado = cita.estado,
                ticket = cita.ticket,
                horaFin = cita.horaFin.take(5),
                precio = cita.precio
            )
        }.toMutableStateList()
    }

    val citaACancelar = cancelacion.cita
    var citaAReprogramar by remember { mutableStateOf<CitaUI?>(null) }

    // Estados para reprogramación (T31.2)
    var nuevaFecha by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var nuevoTurno by remember { mutableStateOf("09:00 AM") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Citas") },
                actions = {
                    TextButton(
                        enabled = !estado.cargando && !cancelando,
                        onClick = { viewModel.cargarCitas() }
                    ) { Text("Actualizar") }
                },
                navigationIcon = {
                    IconButton(enabled = !cancelando, onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            estado.aviso?.let { aviso ->
                item { Text(aviso, color = MaterialTheme.colorScheme.primary) }
            }
            when {
                estado.cargando -> item {
                    CircularProgressIndicator()
                    Text("Cargando tus citas...")
                }
                estado.error != null -> item {
                    Text(
                        text = estado.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { viewModel.cargarCitas() }) { Text("Reintentar") }
                }
                listaCitas.isEmpty() -> item {
                    Text("Aún no tienes citas registradas.")
                }
                else -> items(listaCitas, key = { it.id }) { cita ->
                    CitaCard(
                        cita = cita,
                        onCancelar = { viewModel.abrirCancelacion(cita.id) },
                        onReprogramar = { citaAReprogramar = cita },
                        accionesHabilitadas = false,
                        cancelarHabilitado = !cancelando && !estado.cargando && viewModel.puedeCancelar(cita.id)
                    )
                }
            }
        }
    }

    // T32.2: El estado cambia únicamente tras confirmación del servidor.
    citaACancelar?.let { cita ->
        AlertDialog(
            onDismissRequest = { viewModel.cerrarCancelacion() },
            title = { Text("Cancelar cita") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("¿Deseas cancelar esta cita?")
                    Text("Ticket: ${cita.ticket}")
                    Text("Servicio: ${cita.servicio}")
                    Text("Fecha: ${cita.fecha} - ${cita.horaInicio.take(5)}")
                    Text("Solo se puede cancelar con al menos 2 horas de anticipación.")
                    if (cancelando) {
                        CircularProgressIndicator()
                        Text(if (cancelacion.verificando) "Comprobando estado..." else "Cancelando cita...")
                    }
                    cancelacion.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !cancelando &&
                            (cancelacion.requiereVerificacion || viewModel.puedeCancelar(cita.idCita)),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = { viewModel.confirmarCancelacion() }
                ) {
                    Text(if (cancelacion.requiereVerificacion) "Comprobar estado" else "Sí, cancelar")
                }
            },
            dismissButton = {
                TextButton(enabled = !cancelando, onClick = { viewModel.cerrarCancelacion() }) {
                    Text("Volver")
                }
            }
        )
    }

    // T31.2: Diálogo de Reprogramación de Cita
    citaAReprogramar?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaAReprogramar = null },
            title = { Text("Reprogramar Cita") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Selecciona una nueva fecha y turno para tu cita de ${cita.servicio}:")

                    Text("Nueva Fecha:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val hoy = LocalDate.now()
                        (1..3).forEach { i ->
                            val dia = hoy.plusDays(i.toLong())
                            val esSel = dia == nuevaFecha
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (esSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { nuevaFecha = dia }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${dia.dayOfMonth} ${dia.format(DateTimeFormatter.ofPattern("MMM", Locale("es")))}",
                                    color = if (esSel) Color.White else Color.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Text("Nuevo Turno:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("08:00 AM", "11:00 AM", "03:00 PM").forEach { turno ->
                            val esSel = turno == nuevoTurno
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (esSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { nuevoTurno = turno }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = turno,
                                    color = if (esSel) Color.White else Color.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = false,
                    onClick = {
                        val index = listaCitas.indexOfFirst { it.id == cita.id }
                        if (index != -1) {
                            listaCitas[index] = cita.copy(
                                fecha = nuevaFecha.toString(),
                                hora = nuevoTurno
                            )
                        }
                        citaAReprogramar = null
                    }
                ) {
                    Text("Confirmar Cambio")
                }
            },
            dismissButton = {
                TextButton(onClick = { citaAReprogramar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CitaCard(
    cita: CitaUI,
    onCancelar: () -> Unit,
    onReprogramar: () -> Unit,
    accionesHabilitadas: Boolean = false,
    cancelarHabilitado: Boolean = false
) {
    val (estadoColor, estadoTexto) = when (cita.estado.uppercase(Locale.ROOT)) {
        "CONFIRMADA" -> Color(0xFF2E7D32) to "CONFIRMADA"
        "COMPLETADA" -> Color(0xFF1976D2) to "COMPLETADA"
        "PENDIENTE" -> Color(0xFF8A5700) to "PENDIENTE"
        "RECHAZADA" -> Color(0xFFB3261E) to "RECHAZADA"
        "CANCELADA" -> Color(0xFFD32F2F) to "CANCELADA"
        else -> MaterialTheme.colorScheme.onSurfaceVariant to cita.estado
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = cita.servicio, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(estadoColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = estadoTexto, color = estadoColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (cita.ticket.isNotBlank()) {
                Text("Ticket: ${cita.ticket}", fontWeight = FontWeight.SemiBold)
            }
            cita.precio?.let { precio ->
                Text("Precio: S/ ${precio.setScale(2, RoundingMode.HALF_UP).toPlainString()}")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = cita.vehiculo, fontSize = 13.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Mecánico: ${cita.mecanico}", fontSize = 13.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "${cita.fecha} - ${cita.hora}" + if (cita.horaFin.isNotBlank()) " a ${cita.horaFin}" else "", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            if (cita.estado == "CONFIRMADA" || cita.estado == "PENDIENTE") {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        enabled = cancelarHabilitado,
                        onClick = onCancelar,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancelar", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(enabled = accionesHabilitadas, onClick = onReprogramar) {
                        Text("Reprogramar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}