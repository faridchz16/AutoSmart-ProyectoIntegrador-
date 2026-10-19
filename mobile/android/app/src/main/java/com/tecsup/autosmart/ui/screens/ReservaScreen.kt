package com.tecsup.autosmart.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
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
import com.tecsup.autosmart.data.model.MecanicoDisponible
import com.tecsup.autosmart.data.model.Turno
import com.tecsup.autosmart.data.model.VehiculoResponse
import com.tecsup.autosmart.ui.viewmodel.ReservaViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaScreen(
    idServicio: Long = 1L,
    onNavigateBack: () -> Unit = {},
    viewModel: ReservaViewModel = viewModel()
) {
    val mecanicosState by viewModel.mecanicos.collectAsState()
    val turnosState by viewModel.turnos.collectAsState()

    var vehiculoSeleccionado by remember { mutableStateOf<VehiculoResponse?>(null) }
    var mecanicoSeleccionado by remember { mutableStateOf<MecanicoDisponible?>(null) }
    var fechaSeleccionada by remember { mutableStateOf(LocalDate.now()) }
    var turnoSeleccionado by remember { mutableStateOf<Turno?>(null) }

    var mostrarResumen by remember { mutableStateOf(false) }
    var ticketGenerado by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(idServicio) {
        viewModel.cargarMecanicos(idServicio)
    }

    LaunchedEffect(mecanicoSeleccionado, fechaSeleccionada) {
        mecanicoSeleccionado?.let { mecanico ->
            viewModel.cargarTurnos(
                idServicio = idServicio,
                fecha = fechaSeleccionada.toString(),
                idMecanico = mecanico.idMecanico
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservar Cita") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // T27.1: Selector de Vehículo
            item {
                Text(
                    text = "1. Vehículo Seleccionado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Simulación/Selección básica con la estructura existente
                VehiculoCardSimple(
                    marca = "Toyota",
                    modelo = "Corolla",
                    placa = "ABC-123",
                    seleccionado = true,
                    onSelect = { }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "2. Selecciona un Mecánico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (mecanicosState.cargando) {
                item { CircularProgressIndicator() }
            } else if (mecanicosState.error != null) {
                item {
                    Text(
                        text = mecanicosState.error ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                items(mecanicosState.lista) { mecanico ->
                    MecanicoCard(
                        mecanico = mecanico,
                        seleccionado = mecanicoSeleccionado?.idMecanico == mecanico.idMecanico,
                        onSelect = {
                            mecanicoSeleccionado = mecanico
                            turnoSeleccionado = null
                        }
                    )
                }
            }

            // T25.2: Selector de Fecha y Hora
            if (mecanicoSeleccionado != null) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "3. Selecciona Fecha y Hora",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hoy = LocalDate.now()
                        (0..4).forEach { i ->
                            val dia = hoy.plusDays(i.toLong())
                            val esSeleccionado = dia == fechaSeleccionada

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (esSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        fechaSeleccionada = dia
                                        turnoSeleccionado = null
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dia.format(DateTimeFormatter.ofPattern("EEE", Locale("es"))).uppercase(),
                                        fontSize = 12.sp,
                                        color = if (esSeleccionado) Color.White else Color.Black
                                    )
                                    Text(
                                        text = dia.dayOfMonth.toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (esSeleccionado) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Turnos Disponibles",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (turnosState.cargando) {
                    item { CircularProgressIndicator() }
                } else if (turnosState.lista.isEmpty()) {
                    item { Text("No hay turnos disponibles para esta fecha") }
                } else {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .height(160.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(turnosState.lista) { turno ->
                                TurnoChip(
                                    turno = turno,
                                    seleccionado = turnoSeleccionado?.hora == turno.hora,
                                    onSelect = { if (turno.disponible) turnoSeleccionado = turno }
                                )
                            }
                        }
                    }
                }

                // T26.2 y T28.1: Confirmación de reserva
                if (turnoSeleccionado != null) {
                    item {
                        Button(
                            onClick = { mostrarResumen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text("Ver Resumen y Confirmar Reserva")
                        }
                    }
                }
            }
        }
    }

    // T28.1: Diálogo de Ticket de Confirmación
    if (mostrarResumen) {
        AlertDialog(
            onDismissRequest = { mostrarResumen = false },
            title = { Text("Resumen de la Cita", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🚗 Vehículo: Toyota Corolla (ABC-123)")
                    Text("👨‍🔧 Mecánico: ${mecanicoSeleccionado?.nombre}")
                    Text("📅 Fecha: $fechaSeleccionada")
                    Text("⏰ Turno: ${turnoSeleccionado?.hora}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    if (ticketGenerado != null) {
                        Text(
                            text = "Ticket: $ticketGenerado",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (ticketGenerado == null) {
                    Button(
                        onClick = {
                            // Genera el código de ticket localmente para la demostración
                            ticketGenerado = "AS-2026-${(1000..9999).random()}"
                        }
                    ) {
                        Text("Confirmar Cita")
                    }
                } else {
                    Button(
                        onClick = {
                            mostrarResumen = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Finalizar")
                    }
                }
            },
            dismissButton = {
                if (ticketGenerado == null) {
                    TextButton(onClick = { mostrarResumen = false }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }
}

@Composable
fun VehiculoCardSimple(
    marca: String,
    modelo: String,
    placa: String,
    seleccionado: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (seleccionado) 2.dp else 0.dp,
                color = if (seleccionado) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "$marca $modelo", fontWeight = FontWeight.Bold)
                Text(text = "Placa: $placa", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MecanicoCard(
    mecanico: MecanicoDisponible,
    seleccionado: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (seleccionado) 2.dp else 0.dp,
                color = if (seleccionado) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = mecanico.nombre, fontWeight = FontWeight.Bold)
                Text(text = "Experiencia: ${mecanico.aniosExperiencia} años", fontSize = 12.sp)
                Text(
                    text = "Esp: ${mecanico.especialidades.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun TurnoChip(
    turno: Turno,
    seleccionado: Boolean,
    onSelect: () -> Unit
) {
    val backgroundColor = when {
        !turno.disponible -> Color.LightGray
        seleccionado -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        !turno.disponible -> Color.Gray
        seleccionado -> Color.White
        else -> Color.Black
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(enabled = turno.disponible) { onSelect() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = turno.hora,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}