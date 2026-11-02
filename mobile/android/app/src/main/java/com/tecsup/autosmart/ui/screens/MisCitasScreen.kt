package com.tecsup.autosmart.ui.screens

import androidx.compose.runtime.saveable.rememberSaveable
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneOffset
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
    val reprogramacion by viewModel.reprogramacion.collectAsState()
    val reprogramando = reprogramacion.enviando || reprogramacion.verificando
    val procesando = cancelando || reprogramando
    BackHandler(enabled = reprogramacion.cita != null) {
        if (!procesando) viewModel.cerrarReprogramacion()
    }
    BackHandler(enabled = cancelacion.cita != null) {
        if (!procesando) viewModel.cerrarCancelacion()
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Citas") },
                actions = {
                    TextButton(
                        enabled = !estado.cargando && !procesando && reprogramacion.cita == null,
                        onClick = { viewModel.cargarCitas() }
                    ) { Text("Actualizar") }
                },
                navigationIcon = {
                    IconButton(enabled = !procesando, onClick = onNavigateBack) {
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
                        onReprogramar = { viewModel.abrirReprogramacion(cita.id) },
                        accionesHabilitadas = !procesando && viewModel.puedeReprogramar(cita.id),
                        cancelarHabilitado = !procesando && !estado.cargando && viewModel.puedeCancelar(cita.id)
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
                    enabled = !procesando &&
                            (cancelacion.requiereVerificacion || viewModel.puedeCancelar(cita.idCita)),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = { viewModel.confirmarCancelacion() }
                ) {
                    Text(if (cancelacion.requiereVerificacion) "Comprobar estado" else "Sí, cancelar")
                }
            },
            dismissButton = {
                TextButton(enabled = !procesando, onClick = { viewModel.cerrarCancelacion() }) {
                    Text("Volver")
                }
            }
        )
    }

    reprogramacion.cita?.let { ReprogramacionDialog(viewModel) }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReprogramacionDialog(viewModel: MisCitasViewModel) {
    val estado by viewModel.reprogramacion.collectAsState()
    val cita = estado.cita ?: return
    val ocupado = estado.enviando || estado.verificando
    var elegirFecha by rememberSaveable(cita.idCita) { mutableStateOf(false) }
    var elegirHora by rememberSaveable(cita.idCita) { mutableStateOf(false) }
    if (elegirFecha) {
        val selector = rememberDatePickerState(initialSelectedDateMillis =
            LocalDate.parse(estado.fecha).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { elegirFecha = false }, confirmButton = {
            TextButton(onClick = {
                selector.selectedDateMillis?.let {
                    viewModel.editarReprogramacion(fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                }
                elegirFecha = false
            }, enabled = selector.selectedDateMillis != null) { Text("Elegir fecha") }
        }, dismissButton = { TextButton(onClick = { elegirFecha = false }) { Text("Volver") } }) {
            DatePicker(state = selector)
        }
    }
    if (elegirHora) {
        val actual = LocalTime.parse(estado.hora)
        val selector = rememberTimePickerState(initialHour = actual.hour, initialMinute = actual.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { elegirHora = false }, title = { Text("Nueva hora") },
            text = { TimeInput(state = selector) }, confirmButton = {
                TextButton(onClick = {
                    viewModel.editarReprogramacion(hora = LocalTime.of(selector.hour, selector.minute).toString())
                    elegirHora = false
                }) { Text("Elegir hora") }
            }, dismissButton = { TextButton(onClick = { elegirHora = false }) { Text("Volver") } })
    }
    AlertDialog(onDismissRequest = viewModel::cerrarReprogramacion,
        title = { Text(if (estado.confirmando) "Confirmar reprogramación" else "Reprogramar cita") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ticket: ${cita.ticket}")
                Text("Horario actual: ${cita.fecha} ${cita.horaInicio.take(5)}")
                if (estado.confirmando || estado.requiereVerificacion) {
                    Text("Nuevo horario: ${estado.fecha} ${estado.hora}")
                    Text("El cambio quedará pendiente de aceptación por el mecánico.")
                } else {
                    TextButton(onClick = { elegirFecha = true }, enabled = !ocupado) { Text("Nueva fecha: ${estado.fecha}") }
                    TextButton(onClick = { elegirHora = true }, enabled = !ocupado) { Text("Nueva hora: ${estado.hora}") }
                    Text("Selecciona fecha y hora. La disponibilidad se comprobará al confirmar.")
                    Text("Solo se puede reprogramar con al menos 24 horas de anticipación a la cita actual.")
                }
                if (ocupado) { CircularProgressIndicator(); Text(if (estado.verificando) "Comprobando estado..." else "Reprogramando...") }
                estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (estado.confirmando && !estado.requiereVerificacion) {
                    TextButton(onClick = { viewModel.editarReprogramacion() }, enabled = !ocupado) { Text("Volver a editar") }
                }
            }
        }, confirmButton = {
            Button(enabled = !ocupado, onClick = {
                when {
                    estado.requiereVerificacion -> viewModel.comprobarReprogramacion()
                    estado.confirmando -> viewModel.confirmarReprogramacion()
                    else -> viewModel.solicitarConfirmacionReprogramacion()
                }
            }) { Text(when { estado.requiereVerificacion -> "Comprobar estado"; estado.confirmando -> "Sí, reprogramar"; else -> "Continuar" }) }
        }, dismissButton = {
            TextButton(onClick = viewModel::cerrarReprogramacion, enabled = !ocupado) { Text("Volver a mis citas") }
        })
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