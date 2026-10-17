package com.tecsup.autosmart.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiculosScreen(
    onLogout: () -> Unit,
    onNavigateToReserva: () -> Unit = {} // <-- Callback para navegar a la reserva
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Vehículos", fontWeight = FontWeight.Bold) },
                actions = {
                    // Botón para acceder a la Reserva de Citas
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("No tienes vehículos registrados aún.\nPresiona '+' para agregar uno.")
        }
    }
}