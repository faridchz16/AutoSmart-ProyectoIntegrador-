package com.tecsup.autosmart.ui.mecanico

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.viewmodel.AgendaMecanicoViewModel
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaMecanicoScreen(
    viewModel: AgendaMecanicoViewModel,
    onBack: () -> Unit,
    onDetalle: (Long) -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    var elegirFecha by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    LaunchedEffect(viewModel) { viewModel.cargarSiNecesario() }
    if (elegirFecha) {
        val selector = rememberDatePickerState(initialSelectedDateMillis =
            LocalDate.parse(estado.fecha).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { elegirFecha = false }, confirmButton = {
            TextButton(enabled = selector.selectedDateMillis != null, onClick = {
                selector.selectedDateMillis?.let {
                    viewModel.cargarFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                }
                elegirFecha = false
            }) { Text("Consultar") }
        }, dismissButton = {
            TextButton(onClick = { elegirFecha = false }) { Text("Volver") }
        }) { DatePicker(state = selector) }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Citas asignadas") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al panel")
            }
        }, actions = {
            TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando) { Text("Actualizar") }
        })
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Consulta las citas asignadas para la fecha seleccionada.",
                    style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { elegirFecha = true }) { Text("Fecha: ${estado.fecha}") }
            }
            when {
                estado.cargando -> item { CircularProgressIndicator(); Text("Cargando agenda...") }
                estado.error != null -> item {
                    Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::actualizar) { Text("Reintentar") }
                }
                estado.citas.isEmpty() -> item { Text("No tienes citas asignadas para esta fecha.") }
                else -> items(estado.citas, key = { it.idCita }) { cita ->
                    ElevatedCard(onClick = { onDetalle(cita.idCita) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(cita.servicio.datoCita(), style = MaterialTheme.typography.titleMedium)
                            Text("Ticket: ${cita.ticket.datoCita()}")
                            Text("Cliente: ${cita.cliente.datoCita()}")
                            Text("Placa: ${cita.placa.datoCita()}")
                            Text("Horario: ${cita.horaInicio?.take(5).datoCita()} – ${cita.horaFin?.take(5).datoCita()}")
                            Text("Estado: ${cita.estado.datoCita()}")
                            Text("Ver detalle", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

internal fun String?.datoCita(): String = this?.takeIf { it.isNotBlank() } ?: "No disponible"
