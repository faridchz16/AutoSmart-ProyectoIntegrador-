package com.tecsup.autosmart.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.ui.viewmodel.PresupuestoViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresupuestoScreen(idCita: Long, viewModel: PresupuestoViewModel, onBack: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    BackHandler(onBack = onBack)
    LaunchedEffect(idCita) { viewModel.iniciar(idCita) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Presupuesto") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a Mis citas") }
        }, actions = { TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando) { Text("Actualizar") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Cita: $idCita") }
            val p = estado.presupuesto
            when {
                estado.cargando -> item { CircularProgressIndicator(); Text("Consultando presupuesto...") }
                estado.error != null -> item {
                    Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::actualizar) { Text("Reintentar") }
                }
                p == null -> item { Text("Esta cita aún no tiene presupuesto.") }
                else -> {
                    item {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Presupuesto ${p.idPresupuesto}", style = MaterialTheme.typography.titleLarge)
                                Text("Orden: ${p.idOrden}")
                                Text("Estado: ${when (p.estado) {
                                    "PENDIENTE" -> "Pendiente de aprobación"; "APROBADO" -> "Aprobado"; "RECHAZADO" -> "Rechazado"; else -> p.estado.datoCita()
                                }}")
                                Text(p.descripcion.datoCita())
                                Text("Creado: ${p.fechaCreacion?.replace('T', ' ').datoCita()}")
                                p.fechaDecision?.takeIf { it.isNotBlank() }?.let { Text("Decisión: ${it.replace('T', ' ')}") }
                            }
                        }
                    }
                    item { Text("Detalle del presupuesto", style = MaterialTheme.typography.titleMedium) }
                    items(p.items.orEmpty()) { item ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (item.tipo == "MANO_OBRA") "Mano de obra" else "Repuesto")
                                Text(item.descripcion.datoCita())
                                Text("Cantidad: ${item.cantidad}")
                                Text("Precio unitario: ${item.precioUnitario.importePresupuesto()}")
                                Text("Subtotal: ${item.subtotal.importePresupuesto()}")
                            }
                        }
                    }
                    item { Text("Total: ${p.total.importePresupuesto()}", style = MaterialTheme.typography.titleLarge) }
                    item { Text("Deficiencias y evidencias", style = MaterialTheme.typography.titleMedium) }
                    if (p.deficiencias.isNullOrEmpty()) item { Text("El presupuesto no incluye deficiencias registradas.") }
                    items(p.deficiencias.orEmpty(), key = { it.idDeficiencia }) { deficiencia ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Deficiencia ${deficiencia.idDeficiencia}")
                                Text("Categoría: ${deficiencia.categoria.etiquetaDeficiencia()}")
                                Text("Severidad: ${deficiencia.severidad.etiquetaDeficiencia()}")
                                Text(deficiencia.descripcion.datoCita())
                                if (deficiencia.fotos.isNullOrEmpty()) Text("Sin fotos adjuntas.")
                                deficiencia.fotos.orEmpty().forEach { foto ->
                                    ImagenEvidencia("presupuesto:${p.idPresupuesto}:foto:${foto.idFoto}",
                                        "Foto ${foto.idFoto} del presupuesto") { viewModel.obtenerFoto(foto.idFoto) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun BigDecimal?.importePresupuesto(): String = this?.let { "S/ ${it.setScale(2, RoundingMode.HALF_UP).toPlainString()}" } ?: "No disponible"
