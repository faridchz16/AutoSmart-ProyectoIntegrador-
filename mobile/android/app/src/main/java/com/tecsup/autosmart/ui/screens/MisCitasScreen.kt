package com.tecsup.autosmart.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.autosmart.ui.viewmodel.ReservaViewModel

// Modelo temporal UI
data class CitaUI(
    val id: Long,
    val servicio: String,
    val vehiculo: String,
    val mecanico: String,
    val fecha: String,
    val hora: String,
    val estado: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisCitasScreen(
    onNavigateBack: () -> Unit = {},
    onReprogramarClick: (Long) -> Unit = {},
    viewModel: ReservaViewModel = viewModel() // Inyección MVVM
) {
    // Lista temporal para renderizar en UI
    val listaCitas = remember {
        mutableStateListOf(
            CitaUI(1L, "Mantenimiento 10,000 km", "Toyota Corolla - ABC-123", "Juan Pérez", "2026-10-25", "10:00 AM", "CONFIRMADA"),
            CitaUI(2L, "Cambio de Aceite", "Nissan Sentra - XYZ-789", "Carlos Gómez", "2026-10-28", "02:30 PM", "CONFIRMADA"),
            CitaUI(3L, "Revisión de Frenos", "Toyota Corolla - ABC-123", "Juan Pérez", "2026-09-15", "09:00 AM", "COMPLETADA")
        )
    }

    var citaACancelar by remember { mutableStateOf<CitaUI?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Citas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (listaCitas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No tienes citas registradas.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listaCitas) { cita ->
                    CitaCard(
                        cita = cita,
                        onCancelar = { citaACancelar = cita },
                        onReprogramar = { onReprogramarClick(cita.id) }
                    )
                }
            }
        }
    }

    // T32.2: Diálogo de Cancelación
    citaACancelar?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaACancelar = null },
            title = { Text("Cancelar Cita") },
            text = { Text("¿Estás seguro de que deseas cancelar la cita de ${cita.servicio}? El turno será liberado.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val index = listaCitas.indexOfFirst { it.id == cita.id }
                        if (index != -1) {
                            listaCitas[index] = cita.copy(estado = "CANCELADA")
                        }
                        citaACancelar = null
                    }
                ) {
                    Text("Sí, Cancelar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { citaACancelar = null }) {
                    Text("Volver")
                }
            }
        )
    }
}

@Composable
fun CitaCard(
    cita: CitaUI,
    onCancelar: () -> Unit,
    onReprogramar: () -> Unit
) {
    val (estadoColor, estadoTexto) = when (cita.estado) {
        "CONFIRMADA" -> Color(0xFF2E7D32) to "CONFIRMADA"
        "COMPLETADA" -> Color(0xFF1976D2) to "COMPLETADA"
        else -> Color(0xFFD32F2F) to "CANCELADA"
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
                Text(text = cita.servicio, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(estadoColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = estadoTexto, color = estadoColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
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
                Text(text = "${cita.fecha} - ${cita.hora}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            if (cita.estado == "CONFIRMADA") {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCancelar,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancelar", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onReprogramar) {
                        Text("Reprogramar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}