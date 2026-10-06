package com.tecsup.autosmart.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.data.model.VehiculoRequest
import com.tecsup.autosmart.data.model.VehiculoResponse
import com.tecsup.autosmart.data.network.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiculosScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var vehiculos by remember { mutableStateOf<List<VehiculoResponse>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var mostrarDialogo by remember { mutableStateOf(false) }

    fun cargarVehiculos() {
        coroutineScope.launch {
            cargando = true
            try {
                val res = RetrofitClient.vehiculoApi.listarVehiculos()
                if (res.isSuccessful) {
                    vehiculos = res.body() ?: emptyList()
                } else {
                    Toast.makeText(context, "Error al cargar: ${res.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(Unit) {
        cargarVehiculos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Vehículos", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogo = true }
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cargando && vehiculos.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (vehiculos.isEmpty()) {
                Text(
                    text = "No tienes vehículos registrados aún.\nPresiona '+' para agregar uno.",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 16.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(vehiculos) { v ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🚗",
                                    fontSize = 32.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column {
                                    Text(
                                        text = "${v.marca} ${v.modelo} (${v.anio})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(text = "Placa: ${v.placa}", fontSize = 14.sp)
                                    Text(text = "Kilometraje: ${v.kilometraje} km", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        var placa by remember { mutableStateOf("") }
        var marca by remember { mutableStateOf("") }
        var modelo by remember { mutableStateOf("") }
        var anio by remember { mutableStateOf("") }
        var kilometraje by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Registrar Vehículo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = placa,
                        onValueChange = { placa = it.uppercase() },
                        label = { Text("Placa") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = marca,
                        onValueChange = { marca = it },
                        label = { Text("Marca") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = modelo,
                        onValueChange = { modelo = it },
                        label = { Text("Modelo") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = anio,
                        onValueChange = { anio = it },
                        label = { Text("Año") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = kilometraje,
                        onValueChange = { kilometraje = it },
                        label = { Text("Kilometraje") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (placa.isNotBlank() && marca.isNotBlank() && modelo.isNotBlank() && anio.isNotBlank()) {
                            coroutineScope.launch {
                                try {
                                    val req = VehiculoRequest(
                                        placa = placa.trim(),
                                        marca = marca.trim(),
                                        modelo = modelo.trim(),
                                        anio = anio.toIntOrNull() ?: 2020,
                                        kilometraje = kilometraje.toIntOrNull() ?: 0,
                                        idCliente = 1
                                    )
                                    val res = RetrofitClient.vehiculoApi.registrarVehiculo(req)
                                    if (res.isSuccessful) {
                                        Toast.makeText(context, "¡Vehículo registrado!", Toast.LENGTH_SHORT).show()
                                        mostrarDialogo = false
                                        cargarVehiculos()
                                    } else {
                                        Toast.makeText(context, "Error: ${res.code()} - Placa duplicada o inválida", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Fallo: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}