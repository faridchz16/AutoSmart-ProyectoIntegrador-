package com.tecsup.autosmart.ui.servicios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.ui.cliente.coloresCliente
import com.tecsup.autosmart.ui.components.BarraAutoSmart
import com.tecsup.autosmart.ui.components.formatearDuracion
import com.tecsup.autosmart.ui.components.formatearPrecio
import com.tecsup.autosmart.viewmodel.ServiciosViewModel

@Composable
fun ServicioDetalleScreen(
    idServicio: Long,
    vm: ServiciosViewModel,
    onReservar: () -> Unit,
    onBack: () -> Unit
) {
    val estado by vm.detalle.collectAsState()
    val servicio = estado.servicio
    val c = coloresCliente()

    LaunchedEffect(idServicio) { vm.cargarDetalle(idServicio) }

    Scaffold(
        containerColor = c.fondo,
        topBar = {
            BarraAutoSmart(
                title = { Text("Detalle del servicio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            if (servicio != null) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Button(
                        onClick = onReservar,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.azul, contentColor = Color.White)
                    ) { Text("Reservar este servicio", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                servicio != null -> {
                    val correctivo = servicio.categoria.equals("CORRECTIVO", ignoreCase = true)
                    if (!servicio.categoria.isNullOrBlank()) {
                        val color = if (correctivo) Color(0xFFEA580C) else Color(0xFF16A34A)
                        Text(
                            if (correctivo) "Correctivo" else "Preventivo",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(color.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        servicio.nombre.replaceFirstChar { it.uppercase() },
                        fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = c.texto
                    )
                    BloqueDetalle(Icons.Outlined.Description, "Descripción") {
                        Text(
                            servicio.descripcion?.takeIf { it.isNotBlank() } ?: "Este servicio no tiene descripción.",
                            fontSize = 14.sp, lineHeight = 21.sp, color = c.texto
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BloqueDetalle(Icons.Outlined.Payments, "Precio base", Modifier.weight(1f)) {
                            Text(formatearPrecio(servicio.precioBase), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.azul)
                        }
                        BloqueDetalle(Icons.Outlined.Schedule, "Duración", Modifier.weight(1f)) {
                            Text(formatearDuracion(servicio.duracionMinutos), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.texto)
                        }
                    }
                    Text(
                        "El precio es referencial; el mecánico confirma el costo final tras revisar el vehículo.",
                        fontSize = 12.sp, color = c.textoSecundario
                    )
                }

                estado.cargando -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.azul)
                }

                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                    Text(estado.error ?: "No se pudo cargar el servicio", color = c.rojo, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.cargarDetalle(idServicio) },
                        colors = ButtonDefaults.buttonColors(containerColor = c.azul)
                    ) { Text("Reintentar") }
                }
            }
        }
    }
}

@Composable
private fun BloqueDetalle(
    icono: ImageVector,
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val c = coloresCliente()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = c.superficie),
        border = BorderStroke(1.dp, c.borde)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, tint = c.azul, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.textoSecundario)
            }
            Spacer(Modifier.height(6.dp))
            contenido()
        }
    }
}
