package com.tecsup.autosmart.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.data.model.VehiculoResponse
import com.tecsup.autosmart.ui.viewmodel.VehiculosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiculosScreen(
    onLogout: () -> Unit,
    onEditarVehiculo: (VehiculoResponse) -> Unit = {},
    onNavigateToPerfil: () -> Unit = {},
    onNavigateToMisCitas: () -> Unit = {},
    onNavigateToReserva: () -> Unit = {},
    onNavigateToAgregarVehiculo: () -> Unit = {},
    viewModel: VehiculosViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val vehiculos = estado.vehiculos
    val cargando = estado.cargando
    val error = estado.errorCarga
    val contexto = LocalContext.current
    val actividad = remember(contexto) {
        generateSequence(contexto) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
    }
    LaunchedEffect(viewModel) { viewModel.cargarSiNecesario() }
    DisposableEffect(viewModel, actividad) {
        onDispose { if (actividad?.isChangingConfigurations != true) viewModel.limpiar() }
    }
    BackHandler(enabled = estado.ocupado) { }

    estado.seleccionado?.let { vehiculo ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarConfirmacion,
            title = { Text("Eliminar vehículo") },
            text = {
                Text("¿Deseas eliminar el vehículo ${vehiculo.placa} de tu lista? Esta acción no cancela tus citas existentes.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarEliminacion, enabled = !estado.ocupado) {
                    Text(if (estado.ocupado) "Eliminando..." else "Sí, eliminar",
                        color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cerrarConfirmacion, enabled = !estado.ocupado) {
                    Text("Conservar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onNavigateToPerfil,
                        enabled = !estado.ocupado,
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Mi perfil",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    // Botón para acceder a la Reserva de Citas
                    TextButton(onClick = onNavigateToMisCitas, enabled = !estado.ocupado) {
                        Text("Mis citas")
                    }
                    TextButton(onClick = onNavigateToReserva, enabled = !estado.ocupado) {
                        Text("Reservar", fontWeight = FontWeight.SemiBold)
                    }
                    // Botón Salir
                    TextButton(onClick = onLogout, enabled = !estado.ocupado) {
                        Text("Salir", color = Color.Red)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (!estado.ocupado) onNavigateToAgregarVehiculo() }) {
                Text("+")
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            estado.errorOperacion?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                if (estado.pendienteVerificacion != null) {
                    TextButton(onClick = viewModel::comprobarEstado, enabled = !estado.ocupado) {
                        Text(if (estado.ocupado) "Comprobando..." else "Comprobar estado")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            estado.mensaje?.let {
                Text(it)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (estado.ocupado && estado.seleccionado == null) CircularProgressIndicator()

            when {
                cargando -> {
                    CircularProgressIndicator()
                }

                error != null -> {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = viewModel::cargar) { Text("Reintentar") }
                }

                vehiculos.isEmpty() -> {
                    Text(
                        "No tienes vehículos registrados aún.\n" +
                                "Presiona '+' para agregar uno."
                    )
                }

                else -> {
                    Text(
                        "Mis vehículos registrados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(vehiculos) { vehiculo ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "${vehiculo.marca} ${vehiculo.modelo}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Placa: ${vehiculo.placa}")
                                    Text("Año: ${vehiculo.anio}")
                                    Text("Kilometraje: ${vehiculo.kilometraje} km")
                                    Row {
                                        TextButton(onClick = { onEditarVehiculo(vehiculo) },
                                            enabled = !estado.ocupado && estado.pendienteVerificacion == null) {
                                            Text("Editar")
                                        }
                                        TextButton(onClick = { viewModel.seleccionar(vehiculo) },
                                            enabled = !estado.ocupado && estado.pendienteVerificacion == null) {
                                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
