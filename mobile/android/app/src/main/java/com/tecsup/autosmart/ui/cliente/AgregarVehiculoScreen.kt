package com.tecsup.autosmart.ui.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.model.VehiculoRequest
import com.tecsup.autosmart.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarVehiculoScreen(
    onBack: () -> Unit,
    onVehiculoRegistrado: () -> Unit
) {
    var placa by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var anio by remember { mutableStateOf("") }
    var kilometraje by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar vehículo") },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        enabled = !guardando
                    ) {
                        Text("Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Registrar nuevo vehículo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = placa,
                onValueChange = {
                    placa = it.uppercase()
                    error = null
                },
                label = { Text("Placa") },
                placeholder = { Text("ABC-123") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = marca,
                onValueChange = { marca = it },
                label = { Text("Marca") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = modelo,
                onValueChange = { modelo = it },
                label = { Text("Modelo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = anio,
                onValueChange = { anio = it.filter(Char::isDigit) },
                label = { Text("Año") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = kilometraje,
                onValueChange = {
                    kilometraje = it.filter(Char::isDigit)
                },
                label = { Text("Kilometraje") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando,
                onClick = {
                    val anioNumero = anio.toIntOrNull()
                    val kmNumero = kilometraje.toIntOrNull()
                    val anioMaximo =
                        Calendar.getInstance().get(Calendar.YEAR) + 1

                    error = when {
                        !Regex("^[A-Z0-9]{3}-[0-9]{3}$")
                            .matches(placa.trim()) ->
                            "La placa debe tener el formato ABC-123."

                        marca.isBlank() ->
                            "Ingresa la marca."

                        modelo.isBlank() ->
                            "Ingresa el modelo."

                        anioNumero == null ||
                                anioNumero !in 1900..anioMaximo ->
                            "Ingresa un año válido."

                        kmNumero == null ->
                            "Ingresa un kilometraje válido."

                        else -> null
                    }

                    if (error == null &&
                        anioNumero != null &&
                        kmNumero != null
                    ) {
                        scope.launch {
                            guardando = true
                            try {
                                val request = VehiculoRequest(
                                    placa = placa.trim(),
                                    marca = marca.trim(),
                                    modelo = modelo.trim(),
                                    anio = anioNumero,
                                    kilometraje = kmNumero
                                )

                                val response =
                                    RetrofitClient.vehiculoApi
                                        .registrarVehiculo(request)

                                if (response.isSuccessful) {
                                    onVehiculoRegistrado()
                                } else {
                                    error = when (response.code()) {
                                        400 -> "Datos inválidos. Revisa los campos."
                                        401 -> "Tu sesión expiró."
                                        403 -> "No tienes permiso para registrar vehículos."
                                        409 -> "Esta placa ya está registrada."
                                        else -> "Error HTTP ${response.code()}"
                                    }
                                }
                            } catch (e: Exception) {
                                error = "Error de conexión: ${e.localizedMessage}"
                            } finally {
                                guardando = false
                            }
                        }
                    }
                }
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar vehículo")
                }
            }
        }
    }
}
