package com.tecsup.autosmart.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.data.model.VehiculoResponse
import com.tecsup.autosmart.ui.viewmodel.EditarVehiculoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarVehiculoScreen(
    viewModel: EditarVehiculoViewModel,
    onBack: () -> Unit,
    onActualizado: (VehiculoResponse) -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    val contexto = LocalContext.current
    val actividad = remember(contexto) {
        generateSequence(contexto) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
    }
    DisposableEffect(viewModel, actividad) {
        onDispose { if (actividad?.isChangingConfigurations != true) viewModel.limpiar() }
    }
    BackHandler { if (!estado.guardando) onBack() }
    LaunchedEffect(estado.actualizado) { estado.actualizado?.let(onActualizado) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Editar vehículo") }, navigationIcon = {
            TextButton(onClick = onBack, enabled = !estado.guardando) { Text("Volver") }
        })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (estado.original == null) {
                Text("Vuelve a la lista y selecciona el vehículo que deseas editar.")
            } else {
                OutlinedTextField(value = estado.original!!.placa, onValueChange = {},
                    readOnly = true, label = { Text("Placa (no editable)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = estado.marca, onValueChange = { viewModel.editar(marca = it) },
                    label = { Text("Marca") }, enabled = !estado.guardando,
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = estado.modelo, onValueChange = { viewModel.editar(modelo = it) },
                    label = { Text("Modelo") }, enabled = !estado.guardando,
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = estado.anio, onValueChange = { viewModel.editar(anio = it) },
                    label = { Text("Año") }, enabled = !estado.guardando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = estado.kilometraje,
                    onValueChange = { viewModel.editar(kilometraje = it) },
                    label = { Text("Kilometraje") }, enabled = !estado.guardando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = viewModel::guardar, enabled = !estado.guardando,
                    modifier = Modifier.fillMaxWidth()) {
                    if (estado.guardando) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("Guardar cambios")
                }
            }
        }
    }
}
