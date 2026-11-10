
package com.tecsup.autosmart.ui.servicios

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.autosmart.model.CitaRequest
import com.tecsup.autosmart.model.MecanicoDisponible
import com.tecsup.autosmart.model.Turno
import com.tecsup.autosmart.model.VehiculoResponse
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.ui.components.TicketConfirmacionDialog
import com.tecsup.autosmart.viewmodel.ReservaViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
    val confirmacionState by viewModel.confirmacion.collectAsState()

    var vehiculos by remember {
        mutableStateOf<List<VehiculoResponse>>(emptyList())
    }
    var cargandoVehiculos by remember { mutableStateOf(true) }
    var errorVehiculos by remember { mutableStateOf<String?>(null) }

    var vehiculoSeleccionado by remember {
        mutableStateOf<VehiculoResponse?>(null)
    }
    var mecanicoSeleccionado by remember {
        mutableStateOf<MecanicoDisponible?>(null)
    }
    var fechaSeleccionada by remember {
        mutableStateOf(LocalDate.now())
    }
    var turnoSeleccionado by remember {
        mutableStateOf<Turno?>(null)
    }

    var mostrarResumen by rememberSaveable { mutableStateOf(false) }


    LaunchedEffect(idServicio) {
        viewModel.cargarMecanicos(idServicio)
    }


    LaunchedEffect(Unit) {
        cargandoVehiculos = true
        errorVehiculos = null

        try {
            val respuesta =
                RetrofitClient.vehiculoApi.listarVehiculos()

            if (respuesta.isSuccessful) {
                vehiculos = respuesta.body().orEmpty()
            } else {
                errorVehiculos =
                    "No se pudieron cargar los vehículos. " +
                            "HTTP ${respuesta.code()}"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorVehiculos =
                e.message ?: "Error al conectar con el servidor"
        } finally {
            cargandoVehiculos = false
        }
    }


    LaunchedEffect(
        idServicio,
        mecanicoSeleccionado,
        fechaSeleccionada
    ) {
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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


            item {
                Text(
                    text = "1. Selecciona tu Vehículo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (cargandoVehiculos) {
                item {
                    CircularProgressIndicator()
                }
            } else if (errorVehiculos != null) {
                item {
                    Text(
                        text = errorVehiculos.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (vehiculos.isEmpty()) {
                item {
                    Text(
                        text = "No tienes vehículos disponibles. " +
                                "Registra un vehículo antes de reservar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(
                    items = vehiculos,
                    key = { it.idVehiculo }
                ) { vehiculo ->

                    VehiculoCardSimple(
                        marca = vehiculo.marca,
                        modelo = vehiculo.modelo,
                        placa = vehiculo.placa,
                        seleccionado =
                            vehiculoSeleccionado?.idVehiculo ==
                                    vehiculo.idVehiculo,
                        onSelect = {
                            vehiculoSeleccionado = vehiculo
                        }
                    )
                }
            }


            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = "2. Selecciona un Mecánico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (mecanicosState.cargando) {
                item {
                    CircularProgressIndicator()
                }
            } else if (mecanicosState.error != null) {
                item {
                    Text(
                        text = mecanicosState.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (mecanicosState.lista.isEmpty()) {
                item {
                    Text(
                        text = "No hay mecánicos disponibles " +
                                "para este servicio.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(mecanicosState.lista) { mecanico ->
                    MecanicoCard(
                        mecanico = mecanico,
                        seleccionado =
                            mecanicoSeleccionado?.idMecanico ==
                                    mecanico.idMecanico,
                        onSelect = {
                            mecanicoSeleccionado = mecanico
                            turnoSeleccionado = null
                        }
                    )
                }
            }


            if (mecanicoSeleccionado != null) {

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

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
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        val hoy = LocalDate.now()

                        (0..4).forEach { i ->
                            val dia = hoy.plusDays(i.toLong())
                            val seleccionado =
                                dia == fechaSeleccionada

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (seleccionado)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        fechaSeleccionada = dia
                                        turnoSeleccionado = null
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dia.format(
                                            DateTimeFormatter.ofPattern(
                                                "EEE",
                                                Locale("es")
                                            )
                                        ).uppercase(),
                                        fontSize = 12.sp,
                                        color = if (seleccionado)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = dia.dayOfMonth.toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (seleccionado)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
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
                    item {
                        CircularProgressIndicator()
                    }
                } else if (turnosState.error != null) {
                    item {
                        Text(
                            text = turnosState.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (turnosState.lista.isEmpty()) {
                    item {
                        Text(
                            "No hay turnos disponibles para esta fecha"
                        )
                    }
                } else {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .height(160.dp)
                                .fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            items(turnosState.lista) { turno ->
                                TurnoChip(
                                    turno = turno,
                                    seleccionado =
                                        turnoSeleccionado?.hora ==
                                                turno.hora,
                                    onSelect = {
                                        if (turno.disponible) {
                                            turnoSeleccionado = turno
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (turnoSeleccionado != null) {

                    item {
                        key(
                            mecanicoSeleccionado?.idMecanico,
                            fechaSeleccionada,
                            turnoSeleccionado?.hora
                        ) {
                            if (!confirmacionState.cargando &&
                                confirmacionState.cita == null
                            ) {
                                TemporizadorBloqueoTurno(
                                    tiempoInicialSegundos = 600,
                                    onTiempoAgotado = {
                                        val confirmacionActual = viewModel.confirmacion.value
                                        if (!confirmacionActual.cargando &&
                                            confirmacionActual.cita == null
                                        ) {
                                            turnoSeleccionado = null
                                            mostrarResumen = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                viewModel.limpiarConfirmacion()
                                mostrarResumen = true
                            },
                            enabled =
                                vehiculoSeleccionado != null &&
                                        !turnosState.cargando &&
                                        !confirmacionState.cargando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text("Ver Resumen y Confirmar Reserva")
                        }

                        if (vehiculoSeleccionado == null) {
                            Text(
                                text = "Primero selecciona un vehículo.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }


    val citaCreada = confirmacionState.cita
    if (citaCreada != null) {
        TicketConfirmacionDialog(
            cita = citaCreada,
            onFinalizar = {
                mostrarResumen = false
                viewModel.limpiarConfirmacion()
                onNavigateBack()
            }
        )
    } else if (mostrarResumen) {
        AlertDialog(
            onDismissRequest = {
                if (!confirmacionState.cargando) {
                    mostrarResumen = false
                }
            },
            title = {
                Text("Resumen de la Cita", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🚗 Vehículo: " +
                                "${vehiculoSeleccionado?.marca} " +
                                "${vehiculoSeleccionado?.modelo} " +
                                "(${vehiculoSeleccionado?.placa})"
                    )
                    Text("👨‍🔧 Mecánico: ${mecanicoSeleccionado?.nombre}")
                    Text("📅 Fecha: $fechaSeleccionada")
                    Text("⏰ Turno: ${turnoSeleccionado?.hora}")

                    if (confirmacionState.cargando) {
                        CircularProgressIndicator()
                        Text("Registrando cita...")
                    }
                    confirmacionState.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !confirmacionState.cargando,
                    onClick = {
                        val vehiculo = vehiculoSeleccionado
                        val mecanico = mecanicoSeleccionado
                        val turno = turnoSeleccionado

                        if (vehiculo != null && mecanico != null &&
                            turno != null && turno.disponible
                        ) {
                            val request = CitaRequest(
                                idServicio = idServicio,
                                idMecanico = mecanico.idMecanico,
                                idVehiculo = vehiculo.idVehiculo,
                                fecha = fechaSeleccionada.toString(),
                                horaInicio = turno.hora
                            )
                            viewModel.confirmarCita(request)
                        }
                    }
                ) {
                    Text("Confirmar Cita")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !confirmacionState.cargando,
                    onClick = {
                        mostrarResumen = false
                        viewModel.limpiarConfirmacion()
                    }
                ) {
                    Text("Cancelar")
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
                color = if (seleccionado)
                    MaterialTheme.colorScheme.primary
                else
                    Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
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
                Text(
                    text = "$marca $modelo",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Placa: $placa",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                color = if (seleccionado)
                    MaterialTheme.colorScheme.primary
                else
                    Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
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
                Text(
                    text = mecanico.nombre,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Experiencia: " +
                            "${mecanico.aniosExperiencia} años",
                    fontSize = 12.sp
                )
                Text(
                    text = "Esp: " +
                            mecanico.especialidades.joinToString(", "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        seleccionado -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(enabled = turno.disponible) {
                onSelect()
            }
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


@Composable
fun TemporizadorBloqueoTurno(
    tiempoInicialSegundos: Int = 600,
    onTiempoAgotado: () -> Unit = {}
) {
    var segundosRestantes by remember {
        mutableIntStateOf(tiempoInicialSegundos)
    }

    LaunchedEffect(segundosRestantes) {
        if (segundosRestantes > 0) {
            delay(1000L)
            segundosRestantes--
        } else {
            onTiempoAgotado()
        }
    }

    val minutos = segundosRestantes / 60
    val segundos = segundosRestantes % 60

    val tiempoFormateado = String.format(
        Locale.getDefault(),
        "%02d:%02d",
        minutos,
        segundos
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text = "⏱️ Tiempo para confirmar:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = tiempoFormateado,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
