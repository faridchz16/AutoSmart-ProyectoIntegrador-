package com.tecsup.autosmart.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.ui.components.TarjetaServicio
import com.tecsup.autosmart.viewmodel.ServiciosViewModel

@Composable
fun ServiciosScreen(
    vm: ServiciosViewModel,
    onVerDetalle: (Long) -> Unit,
    onBack: () -> Unit
) {
    val estado by vm.lista.collectAsState()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF070C14), Color(0xFF0F172A), Color(0xFF030712)))),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
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
            Text("Catálogo de servicios", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Elige el servicio que necesitas", fontSize = 14.sp, color = Color(0xFF94A3B8))
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = estado.busqueda,
                onValueChange = { vm.buscar(it) },
                placeholder = { Text("Buscar servicio…", color = Color(0xFF64748B)) },
                leadingIcon = { Text("🔍", fontSize = 16.sp) },
                trailingIcon = if (estado.busqueda.isNotEmpty()) {
                    {
                        IconButton(onClick = { vm.buscar("") }) {
                            Text("✕", color = Color(0xFF94A3B8), fontSize = 16.sp)
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0B1220),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                )
            )

            if (estado.cargando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(2.dp),
                    color = Color(0xFF3B82F6),
                    trackColor = Color.Transparent
                )
            } else {
                Spacer(Modifier.height(10.dp))
            }

            when {
                estado.error != null -> Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text(estado.error ?: "", color = Color(0xFFF87171), fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { vm.reintentar() },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("Reintentar", fontWeight = FontWeight.Bold) }
                }

                estado.cargando && estado.servicios.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Color(0xFF3B82F6)) }

                estado.servicios.isEmpty() -> Text(
                    text = if (estado.busqueda.isBlank()) "Aún no hay servicios disponibles."
                    else "No encontramos servicios con “${estado.busqueda.trim()}”.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
                ) {
                    items(estado.servicios, key = { it.idServicio }) { servicio ->
                        TarjetaServicio(servicio = servicio, onClick = { onVerDetalle(servicio.idServicio) })
                    }
                }
            }
        }
    }
}
