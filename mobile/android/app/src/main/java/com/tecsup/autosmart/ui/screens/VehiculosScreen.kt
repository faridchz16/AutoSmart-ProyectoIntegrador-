package com.tecsup.autosmart.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.data.model.VehiculoResponse
import com.tecsup.autosmart.data.network.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiculosScreen(
    onLogout: () -> Unit,
    onEditarVehiculo: (VehiculoResponse) -> Unit = {},
    onNavigateToPerfil: () -> Unit = {},
    onNavigateToMisCitas: () -> Unit = {},
    onNavigateToReserva: () -> Unit = {} // <-- Callback para navegar a la reserva
) {
    var vehiculos by remember {
        mutableStateOf<List<VehiculoResponse>>(emptyList())
    }

    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        cargando = true
        error = null

        try {
            val response = RetrofitClient.vehiculoApi.listarVehiculos()

            if (response.isSuccessful) {
                vehiculos = response.body().orEmpty()
            } else {
                error = "Error al cargar vehículos: ${response.code()}"
            }
        } catch (e: Exception) {
            error = "Error de conexión: ${e.localizedMessage}"
        } finally {
            cargando = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onNavigateToPerfil,
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
                    TextButton(onClick = onNavigateToMisCitas) {
                        Text("Mis citas")
                    }
                    TextButton(onClick = onNavigateToReserva) {
                        Text("Reservar", fontWeight = FontWeight.SemiBold)
                    }
                    // Botón Salir
                    TextButton(onClick = onLogout) {
                        Text("Salir", color = Color.Red)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Agregar vehículo */ }) {
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
            when {
                cargando -> {
                    CircularProgressIndicator()
                }

                error != null -> {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
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
                                    TextButton(onClick = { onEditarVehiculo(vehiculo) }) {
                                        Text("Editar")
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
