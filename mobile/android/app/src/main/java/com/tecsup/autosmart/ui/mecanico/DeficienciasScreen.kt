package com.tecsup.autosmart.ui.mecanico

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
import com.tecsup.autosmart.model.*
import com.tecsup.autosmart.viewmodel.DeficienciasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeficienciasScreen(idOrden: Long, editable: Boolean, viewModel: DeficienciasViewModel, onBack: () -> Unit,
                       onFotos: (Long) -> Unit) {
    val estado by viewModel.estado.collectAsState()
    var salir by rememberSaveable { mutableStateOf(false) }
    val volver: () -> Unit = { if (!estado.guardando) {
        if (estado.modificado) salir = true else onBack()
    } }
    BackHandler(onBack = volver)
    LaunchedEffect(idOrden) { viewModel.iniciar(idOrden, editable) }
    if (salir) AlertDialog(onDismissRequest = { salir = false }, title = { Text("¿Volver a la orden?") },
        text = { Text(if (estado.verificar) "El registro podría haberse guardado. Consulta la lista antes de crear otro igual."
            else "Los datos del formulario sin registrar se perderán.") },
        confirmButton = { TextButton(onClick = { salir = false; onBack() }) { Text("Volver a la orden") } },
        dismissButton = { TextButton(onClick = { salir = false }) { Text("Continuar aquí") } })
    val habilitado = estado.editable && estado.cargado && !estado.guardando && !estado.cargando && !estado.verificar
    Scaffold(topBar = {
        TopAppBar(title = { Text("Deficiencias") }, navigationIcon = {
            IconButton(onClick = volver, enabled = !estado.guardando) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a la orden")
            }
        }, actions = { TextButton(onClick = viewModel::cargar, enabled = !estado.guardando && !estado.cargando) {
            Text(if (estado.verificar) "Comprobar registro" else "Actualizar")
        } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Orden: $idOrden") }
            if (estado.cargando) item { CircularProgressIndicator(); Text("Consultando deficiencias...") }
            estado.error?.let { error -> item {
                Text(error, color = MaterialTheme.colorScheme.error)
                if (!estado.cargado || estado.verificar) Button(onClick = viewModel::cargar,
                    enabled = !estado.cargando && !estado.guardando) { Text("Comprobar lista") }
            } }
            estado.mensaje?.let { mensaje -> item { Text(mensaje) } }
            if (estado.cargado && !estado.cargando) {
                if (estado.editable) {
                    item { Text("Registrar deficiencia", style = MaterialTheme.typography.titleMedium) }
                    item { SelectorDeficiencia("Categoría", estado.categoria, categoriasDeficiencia, habilitado) {
                        viewModel.editar(categoria = it)
                    } }
                    item { SelectorDeficiencia("Severidad", estado.severidad, severidadesDeficiencia, habilitado) {
                        viewModel.editar(severidad = it)
                    } }
                    item {
                        OutlinedTextField(value = estado.descripcion, onValueChange = { viewModel.editar(descripcion = it) },
                            enabled = habilitado, modifier = Modifier.fillMaxWidth(), minLines = 3,
                            label = { Text("Descripción obligatoria") }, supportingText = { Text("${estado.descripcion.length}/500") })
                        Button(onClick = viewModel::guardar, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
                            Text(if (estado.guardando) "Registrando..." else "Registrar deficiencia")
                        }
                    }
                } else item { Text("Solo consulta: la orden no está en proceso.") }
                item { Text("Deficiencias registradas", style = MaterialTheme.typography.titleMedium) }
                if (estado.lista.isEmpty()) item { Text("Esta orden aún no tiene deficiencias registradas.") }
                items(estado.lista, key = { it.idDeficiencia }) { deficiencia ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Deficiencia ${deficiencia.idDeficiencia}")
                            Text("Categoría: ${deficiencia.categoria.etiquetaDeficiencia()}")
                            Text("Severidad: ${deficiencia.severidad.etiquetaDeficiencia()}")
                            Text(deficiencia.descripcion.datoCita())
                            Text("Registro: ${deficiencia.fechaRegistro?.replace('T', ' ').datoCita()}")
                            TextButton(onClick = { onFotos(deficiencia.idDeficiencia) }, enabled = !estado.guardando) { Text("Fotos de evidencia") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorDeficiencia(label: String, valor: String, opciones: List<String>, habilitado: Boolean, onSelect: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { abierto = true }, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${valor.takeIf { it.isNotBlank() }?.etiquetaDeficiencia() ?: "Seleccionar"}")
        }
        DropdownMenu(expanded = abierto && habilitado, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion -> DropdownMenuItem(text = { Text(opcion.etiquetaDeficiencia()) },
                onClick = { onSelect(opcion); abierto = false }) }
        }
    }
}

internal fun String?.etiquetaDeficiencia(): String = when (this) {
    "MOTOR" -> "Motor"; "FRENOS" -> "Frenos"; "SUSPENSION" -> "Suspensión"; "ELECTRICO" -> "Eléctrico"
    "NEUMATICOS" -> "Neumáticos"; "CARROCERIA" -> "Carrocería"; "FLUIDOS" -> "Fluidos"; "OTRO" -> "Otro"
    "LEVE" -> "Leve"; "MODERADA" -> "Moderada"; "CRITICA" -> "Crítica"; else -> datoCita()
}
