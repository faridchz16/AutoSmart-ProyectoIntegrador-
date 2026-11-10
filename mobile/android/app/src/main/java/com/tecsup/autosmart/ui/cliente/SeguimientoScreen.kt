package com.tecsup.autosmart.ui.cliente

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.viewmodel.SeguimientoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeguimientoScreen(idCita: Long, viewModel: SeguimientoViewModel, onBack: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(idCita) { viewModel.iniciar(idCita) }
    BackHandler(onBack = onBack)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Seguimiento de la cita") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a Mis citas")
                }
            },
            actions = {
                TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando) {
                    Text("Actualizar")
                }
            }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val datos = estado.seguimiento
            val error = estado.error
            when {
                estado.cargando -> item {
                    CircularProgressIndicator()
                    Text("Consultando seguimiento…")
                }
                error != null -> item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::actualizar) { Text("Reintentar") }
                }
                datos != null -> {
                    item {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(datos.servicio.orEmpty(), style = MaterialTheme.typography.titleLarge)
                                Text("Estado: ${datos.estado.orEmpty().replace('_', ' ')}")
                                datos.ticket?.takeIf { it.isNotBlank() }?.let { Text("Ticket: $it") }
                                datos.placa?.let { Text("Placa: $it") }
                                datos.mecanico?.let { Text("Mecánico: $it") }
                                val turno = listOfNotNull(datos.fecha, datos.horaInicio).joinToString(" · ")
                                if (turno.isNotBlank()) Text("Turno: $turno")
                                datos.porcentajeAvance?.let { avance ->
                                    Text("Avance: $avance %")
                                    LinearProgressIndicator(progress = { avance / 100f }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                    item { Text("Línea de tiempo", style = MaterialTheme.typography.titleMedium) }
                    items(datos.lineaDeTiempo.orEmpty().filterNotNull(), key = { it.clave.orEmpty() }) { hito ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(hito.titulo.orEmpty(), style = MaterialTheme.typography.titleMedium)
                                Text(if (hito.completado == true) "Completado" else "Pendiente")
                                hito.fecha?.takeIf { it.isNotBlank() }?.let { Text(it.replace('T', ' ')) }
                                hito.detalle?.takeIf { it.isNotBlank() }?.let { Text(it) }
                            }
                        }
                    }
                }
            }
        }
    }
}
