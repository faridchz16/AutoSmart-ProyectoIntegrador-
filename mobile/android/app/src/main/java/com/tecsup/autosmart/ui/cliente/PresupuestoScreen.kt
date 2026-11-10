package com.tecsup.autosmart.ui.cliente

import com.tecsup.autosmart.ui.mecanico.ImagenEvidencia
import com.tecsup.autosmart.ui.mecanico.datoCita
import com.tecsup.autosmart.ui.mecanico.etiquetaDeficiencia
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.tecsup.autosmart.model.DecisionPresupuesto
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.viewmodel.PresupuestoViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresupuestoScreen(idCita: Long, viewModel: PresupuestoViewModel, onBack: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    var confirmar by rememberSaveable(idCita) { mutableStateOf<DecisionPresupuesto?>(null) }
    val volver: () -> Unit = { if (!estado.enviandoDecision) onBack() }
    BackHandler(onBack = volver)
    LaunchedEffect(idCita) { viewModel.iniciar(idCita) }
    val presupuesto = estado.presupuesto
    confirmar?.let { decision ->
        if (presupuesto?.estado == "PENDIENTE" && estado.errorDecision == null && !estado.enviandoDecision) {
            val accion = if (decision == DecisionPresupuesto.APROBADO) "Aprobar" else "Rechazar"
            AlertDialog(
                onDismissRequest = { confirmar = null },
                title = { Text("¿$accion presupuesto?") },
                text = { Text("Total: ${presupuesto.total.importePresupuesto()}. Esta decisión es definitiva.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmar = null
                        viewModel.decidir(presupuesto.idPresupuesto, decision)
                    }) { Text("Confirmar") }
                },
                dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Volver") } }
            )
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Presupuesto") }, navigationIcon = {
            IconButton(onClick = volver, enabled = !estado.enviandoDecision) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a Mis citas") }
        }, actions = { TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando && !estado.enviandoDecision) { Text("Actualizar") } })
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
                    if (p.estado == "PENDIENTE") item {
                        when {
                            estado.enviandoDecision -> Text("Registrando decisión...")
                            estado.errorDecision != null -> {
                                Text(estado.errorDecision.orEmpty(), color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = viewModel::actualizar) { Text("Consultar estado antes de reintentar") }
                            }
                            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { confirmar = DecisionPresupuesto.APROBADO }) { Text("Aprobar") }
                                OutlinedButton(onClick = { confirmar = DecisionPresupuesto.RECHAZADO }) { Text("Rechazar") }
                            }
                        }
                    }
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
