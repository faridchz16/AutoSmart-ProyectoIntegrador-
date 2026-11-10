package com.tecsup.autosmart.ui.cliente

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.viewmodel.HistorialVehiculoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialVehiculoScreen(
    idVehiculoInicial: Long,
    viewModel: HistorialVehiculoViewModel,
    onBack: () -> Unit,
    onVerDetalle: (Long) -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(idVehiculoInicial) {
        if (viewModel.estado.value.vehiculos.isEmpty()) viewModel.iniciar(idVehiculoInicial)
    }
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de mantenimientos") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (estado.vehiculos.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(estado.vehiculos, key = { it.idVehiculo }) { vehiculo ->
                        FilterChip(
                            selected = estado.seleccionado == vehiculo.idVehiculo,
                            onClick = { if (estado.seleccionado != vehiculo.idVehiculo) viewModel.seleccionar(vehiculo.idVehiculo) },
                            label = { Text(vehiculo.placa) }
                        )
                    }
                }
            }
            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                estado.error != null -> Column(Modifier.padding(16.dp)) {
                    Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = {
                        val id = estado.seleccionado
                        if (id == null) viewModel.iniciar(idVehiculoInicial) else viewModel.seleccionar(id)
                    }) { Text("Reintentar") }
                }
                estado.vehiculos.isEmpty() -> Text("No tienes vehículos registrados.", Modifier.padding(16.dp))
                estado.mantenimientos.isEmpty() -> Text("Este vehículo aún no tiene mantenimientos finalizados.", Modifier.padding(16.dp))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(estado.mantenimientos, key = { it.idOrden }) { mantenimiento ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(mantenimiento.servicio ?: "Mantenimiento", style = MaterialTheme.typography.titleMedium)
                                mantenimiento.ticket?.let { Text("Ticket: $it") }
                                mantenimiento.fechaFin?.let { Text("Finalizado: ${it.replace('T', ' ')}") }
                                mantenimiento.mecanico?.let { Text("Mecánico: $it") }
                                mantenimiento.costoRepuestos?.let { Text("Costo de repuestos: S/ ${it.toPlainString()}") }
                                TextButton(onClick = { onVerDetalle(mantenimiento.idOrden) }) {
                                    Text("Ver detalle")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
