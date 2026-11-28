package com.tecsup.autosmart.ui.servicios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CarRepair
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.OilBarrel
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TireRepair
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.model.Servicio
import com.tecsup.autosmart.ui.cliente.coloresCliente
import com.tecsup.autosmart.ui.components.formatearDuracion
import com.tecsup.autosmart.ui.components.formatearPrecio
import com.tecsup.autosmart.viewmodel.ServiciosViewModel

/** Filtros del catálogo: todos o por categoría que envía el backend. */
private enum class FiltroCategoria(val etiqueta: String, val valor: String?) {
    TODOS("Todos", null),
    PREVENTIVO("Preventivo", "PREVENTIVO"),
    CORRECTIVO("Correctivo", "CORRECTIVO")
}

@Composable
fun ServiciosScreen(
    vm: ServiciosViewModel,
    onVerDetalle: (Long) -> Unit,
    onBack: () -> Unit
) {
    val estado by vm.lista.collectAsState()
    val focusManager = LocalFocusManager.current
    val c = coloresCliente()
    var filtro by rememberSaveable { mutableStateOf(FiltroCategoria.TODOS) }

    // Si quedó un error de antes (por ejemplo, la sesión venció), vuelve a consultar al entrar
    LaunchedEffect(Unit) {
        if (estado.error != null || (!estado.cargando && estado.servicios.isEmpty())) vm.reintentar()
    }

    val visibles = remember(estado.servicios, filtro) {
        if (filtro.valor == null) estado.servicios
        else estado.servicios.filter { it.categoria.equals(filtro.valor, ignoreCase = true) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.fondo)
    ) {
        // Encabezado azul, igual que el inicio del cliente
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0A3AB0), Color(0xFF1150E8), Color(0xFF3B82F6))))
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Catálogo de servicios", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(
                            if (estado.servicios.isEmpty()) "Elige el servicio que necesitas"
                            else "${estado.servicios.size} servicios disponibles",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                TextField(
                    value = estado.busqueda,
                    onValueChange = { vm.buscar(it) },
                    placeholder = { Text("Buscar: aceite, frenos, llantas…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (estado.busqueda.isNotEmpty()) {
                        {
                            IconButton(onClick = { vm.buscar("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = c.superficie,
                        unfocusedContainerColor = c.superficie,
                        focusedTextColor = c.texto,
                        unfocusedTextColor = c.texto,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLeadingIconColor = c.azul,
                        unfocusedLeadingIconColor = c.textoSecundario,
                        focusedPlaceholderColor = c.textoSecundario,
                        unfocusedPlaceholderColor = c.textoSecundario
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp)
                )
            }
        }

        // Filtros por categoría
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FiltroCategoria.entries) { f ->
                val cantidad = if (f.valor == null) estado.servicios.size
                else estado.servicios.count { it.categoria.equals(f.valor, ignoreCase = true) }
                FilterChip(
                    selected = filtro == f,
                    onClick = { filtro = f },
                    label = { Text(if (estado.servicios.isEmpty()) f.etiqueta else "${f.etiqueta} ($cantidad)") },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = c.superficie,
                        labelColor = c.texto,
                        selectedContainerColor = c.azul,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = filtro == f,
                        borderColor = c.borde, selectedBorderColor = c.azul
                    )
                )
            }
        }

        if (estado.cargando && estado.servicios.isNotEmpty()) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(2.dp),
                color = c.azul, trackColor = Color.Transparent
            )
        }

        Box(Modifier.fillMaxSize()) {
            when {
                estado.error != null -> EstadoCentro(
                    icono = Icons.Outlined.WifiOff,
                    titulo = "No se pudo cargar el catálogo",
                    detalle = estado.error.orEmpty(),
                    boton = "Reintentar",
                    onBoton = vm::reintentar
                )

                estado.cargando && estado.servicios.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = c.azul)

                visibles.isEmpty() -> EstadoCentro(
                    icono = Icons.Filled.Search,
                    titulo = if (estado.busqueda.isBlank()) "Aún no hay servicios aquí" else "Sin resultados",
                    detalle = if (estado.busqueda.isBlank()) "Prueba con otra categoría."
                    else "No encontramos servicios con “${estado.busqueda.trim()}”.",
                    boton = if (estado.busqueda.isBlank()) null else "Ver todos",
                    onBoton = { vm.buscar(""); filtro = FiltroCategoria.TODOS }
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.widthIn(max = 760.dp).align(Alignment.TopCenter)
                ) {
                    items(visibles, key = { it.idServicio }) { servicio ->
                        TarjetaCatalogo(servicio) { onVerDetalle(servicio.idServicio) }
                    }
                }
            }
        }
    }
}

/** Ícono según el tipo de servicio (por palabras del nombre). */
private fun iconoDe(nombre: String): ImageVector {
    val n = nombre.lowercase()
    return when {
        listOf("aceite", "filtro", "refrigerante", "lubric").any { it in n } -> Icons.Outlined.OilBarrel
        listOf("llanta", "aline", "balance", "suspens", "neum").any { it in n } -> Icons.Outlined.TireRepair
        listOf("bater", "eléctr", "electr", "alternador").any { it in n } -> Icons.Outlined.BatteryChargingFull
        listOf("aire acond", "a/c", "climat").any { it in n } -> Icons.Outlined.AcUnit
        listOf("diagn", "escáner", "escaner", "inyector", "comput").any { it in n } -> Icons.Outlined.Memory
        listOf("freno", "embrague", "afinamiento", "motor").any { it in n } -> Icons.Outlined.CarRepair
        else -> Icons.Outlined.Build
    }
}

@Composable
private fun TarjetaCatalogo(servicio: Servicio, onClick: () -> Unit) {
    val c = coloresCliente()
    val preventivo = servicio.categoria.equals("PREVENTIVO", ignoreCase = true)
    val correctivo = servicio.categoria.equals("CORRECTIVO", ignoreCase = true)
    val colorCategoria = if (correctivo) Color(0xFFEA580C) else Color(0xFF16A34A)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = c.superficie),
        border = BorderStroke(1.dp, c.borde),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.azulSuave),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconoDe(servicio.nombre), contentDescription = null, tint = c.azul, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        servicio.nombre.replaceFirstChar { it.uppercase() },
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.texto,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (preventivo || correctivo) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (correctivo) "Correctivo" else "Preventivo",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorCategoria,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colorCategoria.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!servicio.descripcion.isNullOrBlank()) {
                    Text(
                        servicio.descripcion,
                        fontSize = 13.sp, lineHeight = 17.sp, color = c.textoSecundario,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatearPrecio(servicio.precioBase), fontWeight = FontWeight.ExtraBold, color = c.azul, fontSize = 15.sp)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = c.textoSecundario, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatearDuracion(servicio.duracionMinutos), color = c.textoSecundario, fontSize = 13.sp)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.textoSecundario)
        }
    }
}

@Composable
private fun BoxScope.EstadoCentro(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    boton: String?,
    onBoton: () -> Unit
) {
    val c = coloresCliente()
    Column(
        Modifier
            .align(Alignment.TopCenter)
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(c.azulSuave),
            contentAlignment = Alignment.Center
        ) { Icon(icono, contentDescription = null, tint = c.azul, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(12.dp))
        Text(titulo, fontWeight = FontWeight.Bold, color = c.texto, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(detalle, color = c.textoSecundario, fontSize = 13.sp)
        if (boton != null) {
            Spacer(Modifier.height(14.dp))
            Button(onClick = onBoton, colors = ButtonDefaults.buttonColors(containerColor = c.azul)) { Text(boton) }
        }
    }
}
