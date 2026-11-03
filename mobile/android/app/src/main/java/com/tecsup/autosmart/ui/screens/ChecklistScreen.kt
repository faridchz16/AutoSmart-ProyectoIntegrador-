package com.tecsup.autosmart.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.ui.viewmodel.ChecklistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(idOrden: Long, editable: Boolean, viewModel: ChecklistViewModel, onBack: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    val volver: () -> Unit = {
        if (!estado.guardando) {
            if (estado.modificado) confirmarSalida = true else onBack()
        }
    }
    BackHandler(onBack = volver)
    LaunchedEffect(idOrden) { viewModel.iniciar(idOrden, editable) }
    if (confirmarSalida) AlertDialog(onDismissRequest = { confirmarSalida = false },
        title = { Text("¿Salir sin guardar?") }, text = { Text("Se perderán los cambios de este checklist.") },
        confirmButton = { TextButton(onClick = { confirmarSalida = false; onBack() }) { Text("Salir sin guardar") } },
        dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Continuar editando") } })
    val habilitado = estado.editable && estado.cargado && !estado.cargando && !estado.guardando
    Scaffold(topBar = {
        TopAppBar(title = { Text("Checklist de recepción") }, navigationIcon = {
            IconButton(onClick = volver, enabled = !estado.guardando) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a la orden")
            }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Orden: $idOrden") }
            if (estado.cargando) item { CircularProgressIndicator(); Text("Consultando checklist...") }
            estado.error?.let { error -> item {
                Text(error, color = MaterialTheme.colorScheme.error)
                if (!estado.cargado) Button(onClick = viewModel::cargar) { Text("Reintentar consulta") }
            } }
            estado.mensaje?.let { mensaje -> item { Text(mensaje) } }
            if (estado.cargado && !estado.cargando) {
                item {
                    if (!estado.editable) Text("Solo consulta: esta orden no está en proceso.")
                    estado.fechaRegistro?.let { Text("Último registro: ${it.replace('T', ' ')}") }
                }
                items(estado.items, key = { it.elemento }) { item ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(when (item.elemento) {
                                "LUCES" -> "Luces"; "FRENOS" -> "Frenos"; "NEUMATICOS" -> "Neumáticos"
                                "FLUIDOS" -> "Fluidos"; else -> "Carrocería"
                            }, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FilterChip(selected = item.estado == "OK", enabled = habilitado,
                                    onClick = { viewModel.cambiarElemento(item.elemento, estado = "OK") }, label = { Text("OK") })
                                FilterChip(selected = item.estado == "OBSERVADO", enabled = habilitado,
                                    onClick = { viewModel.cambiarElemento(item.elemento, estado = "OBSERVADO") }, label = { Text("Observado") })
                            }
                            if (!estado.editable && item.estado == null) Text("Sin registrar")
                            OutlinedTextField(value = item.observacion.orEmpty(),
                                onValueChange = { viewModel.cambiarElemento(item.elemento, observacion = it) },
                                readOnly = !habilitado, modifier = Modifier.fillMaxWidth(),
                                label = { Text(if (item.estado == "OBSERVADO") "Observación obligatoria" else "Observación opcional") },
                                supportingText = { Text("${item.observacion.orEmpty().length}/300") })
                        }
                    }
                }
                item {
                    OutlinedTextField(value = estado.observacionGeneral, onValueChange = viewModel::cambiarGeneral,
                        readOnly = !habilitado, modifier = Modifier.fillMaxWidth(), label = { Text("Observación general opcional") },
                        supportingText = { Text("${estado.observacionGeneral.length}/300") })
                    if (estado.editable) Button(onClick = viewModel::guardar,
                        enabled = habilitado && estado.modificado, modifier = Modifier.fillMaxWidth()) {
                        Text(if (estado.guardando) "Guardando..." else "Guardar checklist")
                    }
                }
            }
        }
    }
}
