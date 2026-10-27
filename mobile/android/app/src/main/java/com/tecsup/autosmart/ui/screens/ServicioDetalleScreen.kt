package com.tecsup.autosmart.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.ui.components.ChipInfo
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

    LaunchedEffect(idServicio) { vm.cargarDetalle(idServicio) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF070C14), Color(0xFF0F172A), Color(0xFF030712)))),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) { Text("Atrás", fontSize = 13.sp, fontWeight = FontWeight.Bold) }

                Spacer(Modifier.height(16.dp))

                when {
                    servicio != null -> {
                        Text(servicio.nombre, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChipInfo(formatearPrecio(servicio.precioBase), destacado = true)
                            ChipInfo(formatearDuracion(servicio.duracionMinutos))
                        }

                        Spacer(Modifier.height(24.dp))
                        BloqueDetalle("Descripción") {
                            Text(
                                text = servicio.descripcion?.takeIf { it.isNotBlank() }
                                    ?: "Este servicio no tiene descripción.",
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        BloqueDetalle("Precio base") {
                            Text(
                                formatearPrecio(servicio.precioBase),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        BloqueDetalle("Duración estimada") {
                            Text(
                                formatearDuracion(servicio.duracionMinutos),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    estado.cargando -> Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }

                    else -> Column {
                        Text(estado.error ?: "No se pudo cargar el servicio", color = Color(0xFFF87171), fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { vm.cargarDetalle(idServicio) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("Reintentar", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            if (servicio != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Button(
                        onClick = onReservar,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D4ED8),
                            contentColor = Color.White
                        )
                    ) { Text("Reservar este servicio", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun BloqueDetalle(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), forma)
            .padding(16.dp)
    ) {
        Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
        Spacer(Modifier.height(6.dp))
        contenido()
    }
}