package com.tecsup.autosmart.ui.mecanico

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.R
import com.tecsup.autosmart.model.AgendaMecanicoDto
import com.tecsup.autosmart.ui.auth.fondoAuth
import com.tecsup.autosmart.ui.cliente.coloresCliente
import com.tecsup.autosmart.viewmodel.MecanicoUiState
import com.tecsup.autosmart.viewmodel.MecanicoViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MarcaFuerte = Color(0xFF1150E8)
private val Ambar = Color(0xFFB45309)

/** Inicio del mecánico: saludo con su nombre, resumen del día y accesos a agenda, órdenes y perfil. */
@Composable
fun MecanicoScreen(
    onLogout: () -> Unit,
    onOrdenesClick: (() -> Unit)? = null,
    onPerfilClick: (() -> Unit)? = null,
    onAgendaClick: (() -> Unit)? = null,
    viewModel: MecanicoViewModel? = null
) {
    val estado = viewModel?.estado?.collectAsState()?.value ?: MecanicoUiState(cargando = false)
    LaunchedEffect(viewModel) { viewModel?.cargarSiNecesario() }

    val c = coloresCliente()
    val horizontal = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var confirmarSalida by remember { mutableStateOf(false) }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres salir de tu cuenta?") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; onLogout() }) { Text("Cerrar sesión", color = c.rojo) }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } }
        )
    }

    val destinos = listOf(
        DestinoMecanico("Inicio", Icons.Filled.Home, seleccionado = true, onClick = {}),
        DestinoMecanico("Agenda", Icons.Outlined.CalendarMonth, onClick = onAgendaClick),
        DestinoMecanico("Órdenes", Icons.Outlined.Build, onClick = onOrdenesClick),
        DestinoMecanico("Perfil", Icons.Outlined.Person, onClick = onPerfilClick)
    )

    val contenido: @Composable (Modifier) -> Unit = { m ->
        Column(
            m
                .fillMaxSize()
                .background(c.fondo)
                .verticalScroll(rememberScrollState())
        ) {
            Encabezado(
                estado = estado,
                horizontal = horizontal,
                onPerfil = onPerfilClick,
                onActualizar = { viewModel?.cargar() },
                onSalir = { confirmarSalida = true }
            )
            val mod = Modifier.padding(horizontal = 16.dp).offset(y = if (horizontal) (-40).dp else (-52).dp)
            val izquierda: @Composable (Modifier) -> Unit = { col ->
                Column(col, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TarjetaResumen(estado)
                    if (estado.perfilIncompleto) AvisoPerfil(onPerfilClick)
                }
            }
            val derecha: @Composable (Modifier) -> Unit = { col ->
                Column(col, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CitasDeHoy(estado.citasHoy, onAgendaClick)
                    OrdenesEnCurso(estado, onOrdenesClick)
                }
            }
            if (horizontal) {
                Row(mod, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    izquierda(Modifier.weight(1f))
                    derecha(Modifier.weight(1.3f))
                }
            } else {
                Column(mod, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    izquierda(Modifier)
                    derecha(Modifier)
                }
            }
        }
    }

    if (horizontal) {
        // Horizontal: barra lateral para no quitarle alto al contenido (igual que el cliente)
        Row(Modifier.fillMaxSize().background(c.fondo)) {
            NavigationRail(containerColor = c.superficie, windowInsets = WindowInsets(0, 0, 0, 0)) {
                Spacer(Modifier.weight(1f))
                destinos.forEach { d ->
                    NavigationRailItem(
                        selected = d.seleccionado,
                        onClick = { d.onClick?.invoke() },
                        enabled = d.seleccionado || d.onClick != null,
                        icon = { Icon(d.icono, contentDescription = null) },
                        label = { Text(d.texto, fontSize = 12.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = c.azul, selectedTextColor = c.azul, indicatorColor = c.azulSuave,
                            unselectedIconColor = c.texto, unselectedTextColor = c.texto
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            contenido(Modifier.weight(1f))
        }
    } else {
        Scaffold(
            containerColor = c.fondo,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(
                    containerColor = c.superficie,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                ) {
                    destinos.forEach { d ->
                        NavigationBarItem(
                            selected = d.seleccionado,
                            onClick = { d.onClick?.invoke() },
                            enabled = d.seleccionado || d.onClick != null,
                            icon = { Icon(d.icono, contentDescription = null) },
                            label = { Text(d.texto, fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = c.azul, selectedTextColor = c.azul, indicatorColor = c.azulSuave,
                                unselectedIconColor = c.texto, unselectedTextColor = c.texto
                            )
                        )
                    }
                }
            }
        ) { padding -> contenido(Modifier.padding(padding)) }
    }
}

private class DestinoMecanico(
    val texto: String,
    val icono: ImageVector,
    val seleccionado: Boolean = false,
    val onClick: (() -> Unit)?
)

@Composable
private fun Encabezado(
    estado: MecanicoUiState,
    horizontal: Boolean,
    onPerfil: (() -> Unit)?,
    onActualizar: () -> Unit,
    onSalir: () -> Unit
) {
    val nombre = estado.perfil?.nombre?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
    val hoy = LocalDate.now(ZoneId.of("America/Lima"))
        .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "PE")))
        .replaceFirstChar { it.uppercase() }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .fondoAuth()
    ) {
        Image(
            painter = painterResource(R.drawable.img_welcome_auto),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .height(if (horizontal) 100.dp else 120.dp)
                .offset(x = 40.dp, y = 6.dp)
                .alpha(0.28f)
        )
        Column(
            Modifier.padding(start = 18.dp, end = 12.dp, top = if (horizontal) 12.dp else 18.dp,
                bottom = if (horizontal) 58.dp else 72.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_autosmart_logo), contentDescription = "AutoSmart", modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("AutoSmart", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic)
                    Text("Panel del mecánico", color = Color(0xFFDCE6F8), fontSize = 12.sp)
                }
                IconButton(onClick = onActualizar, enabled = !estado.cargando) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = Color.White)
                }
                MenuCuenta(estado, onPerfil, onSalir)
            }
            Spacer(Modifier.height(if (horizontal) 10.dp else 18.dp))
            Text(
                if (nombre != null) "Hola, $nombre" else "Hola",
                color = Color.White, fontSize = if (horizontal) 24.sp else 28.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(hoy, color = Color(0xFFDCE6F8), fontSize = 14.sp)
        }
    }
}

@Composable
private fun MenuCuenta(estado: MecanicoUiState, onPerfil: (() -> Unit)?, onSalir: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        AvatarMecanico(estado.perfil?.nombre ?: estado.perfil?.correo, 44.dp) { abierto = true }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            estado.perfil?.let { p ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    p.nombre?.let { Text(it, fontWeight = FontWeight.Bold) }
                    p.correo?.let { Text(it, fontSize = 12.sp) }
                }
                HorizontalDivider()
            }
            onPerfil?.let { abrir ->
                DropdownMenuItem(
                    text = { Text("Mi perfil profesional") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    onClick = { abierto = false; abrir() }
                )
            }
            DropdownMenuItem(
                text = { Text("Cerrar sesión", color = coloresCliente().rojo) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = coloresCliente().rojo) },
                onClick = { abierto = false; onSalir() }
            )
        }
    }
}

/** Círculo con las iniciales del nombre (mismo estilo que el avatar del cliente). */
@Composable
fun AvatarMecanico(nombre: String?, tamano: Dp, onClick: (() -> Unit)? = null) {
    val base = nombre?.trim().orEmpty()
    val iniciales = base.split(" ").filter { it.isNotBlank() }.take(2)
        .mapNotNull { it.firstOrNull { ch -> ch.isLetterOrDigit() }?.uppercaseChar() }.joinToString("")
    val paleta = listOf(0xFF1A73E8, 0xFFD93025, 0xFF188038, 0xFFE37400, 0xFF9334E6, 0xFF00897B, 0xFFC2185B, 0xFF3949AB)
    val color = Color(paleta[base.lowercase().hashCode().mod(paleta.size)])
    Box(
        Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(if (iniciales.isNotEmpty()) color else Color.White.copy(alpha = 0.2f))
            .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (iniciales.isNotEmpty()) {
            Text(iniciales, color = Color.White, fontSize = (tamano.value * 0.38f).sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    val c = coloresCliente()
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = c.superficie,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, c.borde),
        modifier = modifier.fillMaxWidth()
    ) { Column(content = contenido) }
}

@Composable
private fun TarjetaResumen(estado: MecanicoUiState) {
    val c = coloresCliente()
    Tarjeta {
        Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Dato(estado.citasHoy?.size, "Citas de hoy", Icons.Outlined.CalendarMonth, estado.cargando, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(44.dp).background(c.borde))
            Dato(estado.ordenesActivas?.size, "Órdenes en curso", Icons.Outlined.Build, estado.cargando, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(44.dp).background(c.borde))
            Dato(estado.perfil?.aniosExperiencia, "Años de exp.", Icons.Outlined.Badge, estado.cargando, Modifier.weight(1f))
        }
        estado.error?.let {
            Text(it, color = c.rojo, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
        }
    }
}

@Composable
private fun Dato(valor: Int?, etiqueta: String, icono: ImageVector, cargando: Boolean, modifier: Modifier) {
    val c = coloresCliente()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icono, contentDescription = null, tint = c.azul, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            valor?.toString() ?: if (cargando) "…" else "–",
            color = c.texto, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
        )
        Text(etiqueta, color = c.textoSecundario, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun AvisoPerfil(onPerfil: (() -> Unit)?) {
    val c = coloresCliente()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Ambar.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Ambar.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = Ambar)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Completa tu perfil", color = Ambar, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Agrega tus especialidades para que los clientes puedan elegirte al reservar.",
                    color = c.texto, fontSize = 13.sp)
            }
            onPerfil?.let { TextButton(onClick = it) { Text("Completar") } }
        }
    }
}

@Composable
private fun CitasDeHoy(citas: List<AgendaMecanicoDto>?, onAgenda: (() -> Unit)?) {
    val c = coloresCliente()
    Tarjeta {
        Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Citas de hoy", color = c.texto, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            onAgenda?.let { TextButton(onClick = it) { Text("Ver agenda") } }
        }
        when {
            citas == null -> Text("Cargando…", color = c.textoSecundario, fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
            citas.isEmpty() -> Text("No tienes citas para hoy. Revisa tu agenda para ver las próximas.",
                color = c.textoSecundario, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
            else -> Column(Modifier.padding(bottom = 8.dp)) {
                citas.take(4).forEach { cita -> FilaCita(cita) }
                if (citas.size > 4) Text("y ${citas.size - 4} más…", color = c.textoSecundario, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun FilaCita(cita: AgendaMecanicoDto) {
    val c = coloresCliente()
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = c.azul, modifier = Modifier.size(16.dp))
            Text(cita.horaInicio?.take(5) ?: "--:--", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(cita.servicio ?: "Servicio", color = c.texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(cita.placa, cita.cliente).joinToString(" · "), color = c.textoSecundario, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        EstadoCita(cita.estado)
    }
}

@Composable
fun EstadoCita(estado: String?) {
    val (color, texto) = when (estado?.uppercase()) {
        "PENDIENTE" -> Color(0xFFB45309) to "Pendiente"
        "CONFIRMADA" -> Color(0xFF15803D) to "Confirmada"
        "EN_PROCESO" -> Color(0xFF1150E8) to "En proceso"
        "ESPERA_APROBACION" -> Color(0xFFB45309) to "Espera aprob."
        "FINALIZADA" -> Color(0xFF0E7490) to "Finalizada"
        "PAGADO" -> Color(0xFF16A34A) to "Pagado"
        "CANCELADA", "RECHAZADA" -> Color(0xFFDC2626) to estado.lowercase().replaceFirstChar { it.uppercase() }
        else -> Color(0xFF64748B) to (estado ?: "-")
    }
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.14f)) {
        Text(texto, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

/** Órdenes que el mecánico tiene en curso (EN_PROCESO o ESPERA_APROBACION). */
@Composable
private fun OrdenesEnCurso(estado: MecanicoUiState, onOrdenes: (() -> Unit)?) {
    val c = coloresCliente()
    val ordenes = estado.ordenesActivas
    Tarjeta {
        Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Órdenes en curso", color = c.texto, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            onOrdenes?.let { TextButton(onClick = it) { Text("Ver todas") } }
        }
        when {
            ordenes == null -> Text("Cargando…", color = c.textoSecundario, fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
            ordenes.isEmpty() -> Text("No tienes órdenes en curso.", color = c.textoSecundario, fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
            else -> Column(Modifier.padding(bottom = 8.dp)) {
                ordenes.take(3).forEach { o ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onOrdenes != null) { onOrdenes?.invoke() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.azulSuave),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.Build, contentDescription = null, tint = c.azul, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(o.servicio ?: "Orden de trabajo", color = c.texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(o.ticket, o.placa).joinToString(" · "), color = c.textoSecundario, fontSize = 13.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        EstadoCita(o.estado)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.textoSecundario)
                    }
                }
                if (ordenes.size > 3) Text("y ${ordenes.size - 3} más…", color = c.textoSecundario, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }
}
