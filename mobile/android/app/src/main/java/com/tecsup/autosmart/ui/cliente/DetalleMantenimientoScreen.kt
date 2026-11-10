package com.tecsup.autosmart.ui.cliente

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tecsup.autosmart.model.DetalleMantenimientoDto
import com.tecsup.autosmart.ui.mecanico.ImagenEvidencia
import com.tecsup.autosmart.ui.mecanico.datoCita
import com.tecsup.autosmart.ui.mecanico.etiquetaDeficiencia
import com.tecsup.autosmart.viewmodel.DetalleMantenimientoViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleMantenimientoScreen(
    idOrden: Long,
    viewModel: DetalleMantenimientoViewModel,
    descripcionVolver: String = "Volver a la orden",
    onBack: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(idOrden) { viewModel.iniciar(idOrden) }
    BackHandler(onBack = onBack)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Detalle del mantenimiento") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = descripcionVolver)
                }
            },
            actions = {
                TextButton(onClick = viewModel::actualizar, enabled = !estado.cargando) { Text("Actualizar") }
            }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val detalle = estado.detalle
            when {
                estado.cargando -> item { CircularProgressIndicator(); Text("Consultando mantenimiento…") }
                estado.error != null -> item {
                    Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::actualizar) { Text("Reintentar") }
                }
                detalle != null -> contenidoDetalle(detalle, viewModel::obtenerFoto)
            }
        }
    }
}

private fun LazyListScope.contenidoDetalle(
    detalle: DetalleMantenimientoDto,
    verFoto: suspend (Long) -> Result<ByteArray>
) {
    item {
        SeccionDetalle(detalle.servicio.datoCita()) {
            Text("Orden: ${detalle.idOrden} · Cita: ${detalle.idCita}")
            Text("Estado: ${detalle.estado.datoCita().replace('_', ' ')}")
            Text("Ticket: ${detalle.ticket.datoCita()}")
            Text("Vehículo: ${detalle.vehiculo.datoCita()} · ${detalle.placa.datoCita()}")
            Text("Mecánico: ${detalle.mecanico.datoCita()}")
            detalle.fechaInicio?.let { Text("Inicio: ${it.replace('T', ' ')}") }
            detalle.fechaFin?.let { Text("Fin: ${it.replace('T', ' ')}") }
        }
    }
    item {
        SeccionDetalle("Diagnóstico") { Text(detalle.diagnostico.datoCita()) }
    }
    item {
        SeccionDetalle("Checklist de recepción") {
            val items = detalle.checklist.orEmpty().filterNotNull()
            if (items.isEmpty()) Text("Sin checklist registrado.")
            items.forEach { item ->
                Text("${item.elemento}: ${item.estado.datoCita()}")
                item.observacion?.takeIf { it.isNotBlank() }?.let { Text(it) }
            }
        }
    }
    item { Text("Deficiencias y evidencias", style = MaterialTheme.typography.titleMedium) }
    val deficiencias = detalle.deficiencias.orEmpty().filterNotNull()
    if (deficiencias.isEmpty()) item { Text("Sin deficiencias registradas.") }
    items(deficiencias) { deficiencia ->
        SeccionDetalle(deficiencia.categoria.etiquetaDeficiencia()) {
            Text(deficiencia.severidad.etiquetaDeficiencia())
            Text(deficiencia.descripcion.datoCita())
            deficiencia.fotos.orEmpty().forEach { foto ->
                ImagenEvidencia("detalle:${detalle.idOrden}:foto:${foto.idFoto}",
                    "Foto ${foto.idFoto} del mantenimiento") { verFoto(foto.idFoto) }
            }
        }
    }
    item {
        SeccionDetalle("Tareas") {
            val tareas = detalle.tareas
            tareas?.porcentajeAvance?.let { Text("Avance: $it %") }
            if (tareas?.completadas != null && tareas.totalTareas != null) {
                Text("${tareas.completadas} de ${tareas.totalTareas} completadas")
            }
            val lista = tareas?.tareas.orEmpty().filterNotNull()
            if (lista.isEmpty()) Text("Sin tareas registradas.")
            lista.forEach { tarea ->
                val estado = when (tarea.completada) {
                    true -> "Completada"
                    false -> "Pendiente"
                    null -> "Estado no disponible"
                }
                Text("${tarea.descripcion.datoCita()} · $estado")
                tarea.fechaCompletada?.let { Text(it.replace('T', ' ')) }
            }
        }
    }
    item {
        SeccionDetalle("Presupuesto") {
            val p = detalle.presupuesto
            if (p == null) {
                Text("Sin presupuesto registrado.")
            } else {
                Text("Presupuesto ${p.idPresupuesto} · ${p.estado.datoCita()}")
                Text(p.descripcion.datoCita())
                p.items.orEmpty().filterNotNull().forEach { item ->
                    Text("${item.descripcion.datoCita()} · Cantidad: ${item.cantidad ?: "No disponible"}")
                    Text("Subtotal: ${item.subtotal.importe()}")
                }
                Text("Total del presupuesto: ${p.total.importe()}")
            }
        }
    }
    item {
        SeccionDetalle("Repuestos consumidos") {
            val repuestos = detalle.repuestosConsumidos.orEmpty().filterNotNull()
            if (repuestos.isEmpty()) Text("Sin consumos registrados.")
            repuestos.forEach { repuesto ->
                Text("${repuesto.codigoMaterial.datoCita()} · Cantidad: ${repuesto.cantidad ?: "No disponible"}")
                Text("Costo unitario: ${repuesto.costoUnitario.importe()}")
                repuesto.fecha?.let { Text(it.replace('T', ' ')) }
            }
        }
    }
    item {
        SeccionDetalle("Costos") {
            Text("Precio del servicio: ${detalle.precioServicio.importe()}")
            Text("Costo de repuestos: ${detalle.costoRepuestos.importe()}")
            Text("Total estimado: ${detalle.totalEstimado.importe()}")
        }
    }
}

@Composable
private fun SeccionDetalle(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            contenido()
        }
    }
}

private fun BigDecimal?.importe(): String = this?.let { "S/ ${it.toPlainString()}" } ?: "No disponible"
