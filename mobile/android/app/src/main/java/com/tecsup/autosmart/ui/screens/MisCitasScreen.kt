package com.tecsup.autosmart.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
    val estado: String
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisCitasScreen(
    onNavigateBack: () -> Unit = {}
) {
    val listaCitas = remember {
        mutableStateListOf(
            CitaUI(1L, "Mantenimiento 10,000 km", "Toyota Corolla - ABC-123", "Juan Pérez", "2026-10-25", "10:00 AM", "CONFIRMADA"),
            CitaUI(2L, "Cambio de Aceite", "Nissan Sentra - XYZ-789", "Carlos Gómez", "2026-10-28", "02:30 PM", "CONFIRMADA"),
            CitaUI(3L, "Revisión de Frenos", "Toyota Corolla - ABC-123", "Juan Pérez", "2026-09-15", "09:00 AM", "COMPLETADA")
        )
    }

    var citaACancelar by remember { mutableStateOf<CitaUI?>(null) }
    var citaAReprogramar by remember { mutableStateOf<CitaUI?>(null) }

    // Estados para reprogramación (T31.2)
    var nuevaFecha by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var nuevoTurno by remember { mutableStateOf("09:00 AM") }

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
                    onReprogramar = { citaAReprogramar = cita }
                )
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

    // T31.2: Diálogo de Reprogramación de Cita
    citaAReprogramar?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaAReprogramar = null },
            title = { Text("Reprogramar Cita") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Selecciona una nueva fecha y turno para tu cita de ${cita.servicio}:")

                    Text("Nueva Fecha:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val hoy = LocalDate.now()
                        (1..3).forEach { i ->
                            val dia = hoy.plusDays(i.toLong())
                            val esSel = dia == nuevaFecha
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (esSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { nuevaFecha = dia }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${dia.dayOfMonth} ${dia.format(DateTimeFormatter.ofPattern("MMM", Locale("es")))}",
                                    color = if (esSel) Color.White else Color.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Text("Nuevo Turno:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("08:00 AM", "11:00 AM", "03:00 PM").forEach { turno ->
                            val esSel = turno == nuevoTurno
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (esSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { nuevoTurno = turno }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = turno,
                                    color = if (esSel) Color.White else Color.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = listaCitas.indexOfFirst { it.id == cita.id }
                        if (index != -1) {
                            listaCitas[index] = cita.copy(
                                fecha = nuevaFecha.toString(),
                                hora = nuevoTurno
                            )
                        }
                        citaAReprogramar = null
                    }
                ) {
                    Text("Confirmar Cambio")
                }
            },
            dismissButton = {
                TextButton(onClick = { citaAReprogramar = null }) {
                    Text("Cancelar")
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