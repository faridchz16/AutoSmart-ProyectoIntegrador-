package com.tecsup.autosmart.ui.mecanico

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.viewmodel.CitaDetalleMecanicoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitaDetalleMecanicoScreen(idCita: Long, viewModel: CitaDetalleMecanicoViewModel, onBack: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    BackHandler(onBack = onBack)
    LaunchedEffect(idCita, viewModel) { viewModel.cargar(idCita) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Detalle de cita") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a citas")
            }
        }, actions = {
            TextButton(onClick = { viewModel.cargar(idCita, forzar = true) },
                enabled = !estado.cargando) { Text("Actualizar") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                estado.cargando || estado.idCita != idCita -> {
                    CircularProgressIndicator(); Text("Cargando cita...")
                }
                estado.error != null -> {
                    Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.cargar(idCita, forzar = true) }) { Text("Reintentar") }
                }
                else -> estado.cita?.takeIf { it.idCita == idCita }?.let { cita ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(cita.servicio.datoCita(), style = MaterialTheme.typography.titleLarge)
                            Text("Ticket: ${cita.ticket.datoCita()}")
                            Text("Estado: ${cita.estado.datoCita()}")
                            Text("Fecha: ${cita.fecha.datoCita()}")
                            Text("Horario: ${cita.horaInicio?.take(5).datoCita()} – ${cita.horaFin?.take(5).datoCita()}")
                            Text("Precio: ${cita.precio?.toPlainString().datoCita()}")
                            Text("Duración: ${cita.duracionMinutos?.let { "$it min" }.datoCita()}")
                            HorizontalDivider()
                            Text("Cliente: ${cita.cliente.datoCita()}")
                            Text("Teléfono: ${cita.telefonoCliente?.takeIf { it.isNotBlank() } ?: "No registrado"}")
                            HorizontalDivider()
                            Text("Placa: ${cita.placa.datoCita()}")
                            Text("Marca: ${cita.marca.datoCita()}")
                            Text("Modelo: ${cita.modelo.datoCita()}")
                            Text("Año: ${cita.anio?.toString().datoCita()}")
                            Text("Kilometraje: ${cita.kilometraje?.let { "$it km" }.datoCita()}")
                        }
                    }
                    Text("La inspección y las órdenes de trabajo aún no están disponibles.")
                }
            }
        }
    }
}
