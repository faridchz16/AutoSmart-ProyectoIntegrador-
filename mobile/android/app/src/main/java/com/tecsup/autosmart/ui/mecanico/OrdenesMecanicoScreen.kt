package com.tecsup.autosmart.ui.mecanico

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.model.OrdenTrabajoDto
import com.tecsup.autosmart.viewmodel.OrdenesMecanicoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdenesMecanicoScreen(
    viewModel: OrdenesMecanicoViewModel,
    onBack: () -> Unit,
    onAgenda: () -> Unit,
    onChecklist: (Long, Boolean) -> Unit,
    onDeficiencias: (Long, Boolean) -> Unit,
    onDetalleMantenimiento: ((Long) -> Unit)? = null
) {
    val estado by viewModel.estado.collectAsState()
    val seleccionada = estado.ordenes.firstOrNull { it.idOrden == estado.seleccionada }
    val volver: () -> Unit = { if (estado.seleccionada != null) viewModel.volverLista() else onBack() }
    BackHandler(onBack = volver)
    LaunchedEffect(viewModel) { viewModel.cargarSiNecesario() }
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (seleccionada == null) "Órdenes de trabajo" else "Detalle de la orden") },
            navigationIcon = { IconButton(onClick = volver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            } }, actions = {
                TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando) { Text("Actualizar") }
            })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (estado.seleccionada == null) item {
                Text("Órdenes activas asignadas a ti.")
                TextButton(onClick = onAgenda) { Text("Ver agenda de citas") }
            }
            when {
                estado.cargando -> item { CircularProgressIndicator(); Text("Cargando órdenes...") }
                estado.error != null -> item {
                    Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::actualizar) { Text("Reintentar") }
                }
                seleccionada != null -> item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        DatosOrden(seleccionada)
                        TextButton(onClick = { onChecklist(seleccionada.idOrden, seleccionada.estado == "EN_PROCESO") },
                            modifier = Modifier.padding(horizontal = 16.dp)) { Text("Checklist de recepción") }
                        TextButton(onClick = { onDeficiencias(seleccionada.idOrden, seleccionada.estado == "EN_PROCESO") },
                            modifier = Modifier.padding(horizontal = 16.dp)) { Text("Deficiencias") }
                        onDetalleMantenimiento?.let { abrir ->
                            TextButton(onClick = { abrir(seleccionada.idOrden) },
                                modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text("Ver mantenimiento")
                            }
                        }
                    }
                }
                estado.ordenes.isEmpty() -> item { Text("No tienes órdenes de trabajo activas.") }
                else -> items(estado.ordenes, key = { it.idOrden }) { orden ->
                    ElevatedCard(onClick = { viewModel.seleccionar(orden.idOrden) },
                        modifier = Modifier.fillMaxWidth()) {
                        DatosOrden(orden)
                        Text("Ver detalle", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DatosOrden(orden: OrdenTrabajoDto) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(orden.servicio.datoCita(), style = MaterialTheme.typography.titleMedium)
        Text("Orden: ${orden.idOrden}")
        Text("Cita: ${orden.idCita}")
        Text("Ticket: ${orden.ticket.datoCita()}")
        Text("Placa: ${orden.placa.datoCita()}")
        Text("Estado: ${when (orden.estado) {
            "EN_PROCESO" -> "En proceso"
            "ESPERA_APROBACION" -> "En espera de aprobación"
            else -> orden.estado.datoCita()
        }}")
        Text("Inicio: ${orden.fechaInicio?.replace('T', ' ').datoCita()}")
    }
}
