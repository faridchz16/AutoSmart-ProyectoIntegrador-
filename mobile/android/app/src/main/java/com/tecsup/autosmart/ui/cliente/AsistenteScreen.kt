package com.tecsup.autosmart.ui.cliente

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.R
import com.tecsup.autosmart.ui.auth.fondoAuth
import com.tecsup.autosmart.viewmodel.AsistenteViewModel
import com.tecsup.autosmart.viewmodel.MensajeChat
import com.tecsup.autosmart.viewmodel.OPCION_MIS_CITAS
import com.tecsup.autosmart.viewmodel.TarjetaChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val MarcaFuerte = Color(0xFF1150E8)

/**
 * Sprint 4 · HU-75/77: Asistente AutoSmart con IA (Gemini vía Firebase AI Logic).
 * Responde con los servicios, vehículos y citas reales del cliente y entiende fotos del auto.
 */
@Composable
fun AsistenteScreen(
    viewModel: AsistenteViewModel,
    onBack: () -> Unit,
    onReservar: () -> Unit,
    onMisCitas: () -> Unit = {}
) {
    val estado by viewModel.estado.collectAsState()
    val c = coloresCliente()
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val lista = rememberLazyListState()

    // Foto desde la galería (sin permisos) o desde la cámara (vista previa, sin guardar archivos)
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            viewModel.adjuntar(withContext(Dispatchers.IO) { leerImagen(contexto, uri) })
        }
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) viewModel.adjuntar(bitmap)
    }

    LaunchedEffect(estado.mensajes.size, estado.escribiendo) {
        lista.animateScrollToItem((estado.mensajes.size + if (estado.escribiendo) 1 else 0).coerceAtLeast(1) - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.fondo)
            .imePadding()
    ) {
        // Encabezado con el avatar del asistente (un carro)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .fondoAuth()
                .padding(start = 4.dp, end = 12.dp, top = 10.dp, bottom = 14.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            AvatarAuto(46.dp, borde = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Asistente AutoSmart", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                    Spacer(Modifier.width(6.dp))
                    Text(if (estado.escribiendo) "Escribiendo…" else "En línea · con IA", color = Color(0xFFDCE6F8), fontSize = 13.sp)
                }
            }
            // T79.2: botón "Hablar con un asesor"
            IconButton(onClick = viewModel::pedirAsesor, enabled = !estado.escribiendo) {
                Icon(Icons.Outlined.SupportAgent, contentDescription = "Hablar con un asesor", tint = Color.White)
            }
            IconButton(onClick = viewModel::limpiar, enabled = !estado.escribiendo) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Nueva conversación", tint = Color.White)
            }
        }

        LazyColumn(
            state = lista,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(estado.mensajes, key = { it.id }) { m ->
                Burbuja(m, onReservar = onReservar, onMisCitas = onMisCitas, onReintentar = viewModel::reintentar)
            }
            if (estado.escribiendo) item(key = "escribiendo") { Escribiendo() }
        }

        // Opciones rápidas (T75.2): las que propone el asistente en su último mensaje
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            items(estado.opcionesActuales) { opcion ->
                SuggestionChip(
                    onClick = { if (opcion == OPCION_MIS_CITAS) onMisCitas() else viewModel.enviar(opcion) },
                    enabled = !estado.escribiendo,
                    label = { Text(opcion, fontSize = 13.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Foto adjunta antes de enviar
        estado.imagen?.let { foto ->
            Box(Modifier.padding(start = 16.dp, bottom = 6.dp)) {
                Image(
                    foto.asImageBitmap(), contentDescription = "Foto adjunta", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, c.borde, RoundedCornerShape(14.dp))
                )
                Surface(
                    onClick = { viewModel.adjuntar(null) }, shape = CircleShape, color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Barra para escribir
        Surface(color = c.superficie, shadowElevation = 8.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = {
                    galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, enabled = !estado.escribiendo) {
                    Icon(Icons.Outlined.Image, contentDescription = "Adjuntar foto de la galería", tint = c.azul)
                }
                IconButton(onClick = { camara.launch(null) }, enabled = !estado.escribiendo) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Tomar foto", tint = c.azul)
                }
                OutlinedTextField(
                    value = estado.texto,
                    onValueChange = viewModel::editarTexto,
                    placeholder = { Text(if (estado.imagen != null) "Pregunta sobre la foto…" else "Escribe tu consulta…") },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                FilledIconButton(
                    onClick = { viewModel.enviar() },
                    enabled = !estado.escribiendo && (estado.texto.isNotBlank() || estado.imagen != null),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MarcaFuerte, contentColor = Color.White)
                ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar") }
            }
        }
    }
}

/** Avatar del asistente: el auto de AutoSmart dentro de un círculo. */
@Composable
private fun AvatarAuto(tamano: androidx.compose.ui.unit.Dp, borde: Boolean = false) {
    Box(
        Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(Color.White)
            .then(if (borde) Modifier.border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(R.drawable.img_welcome_auto),
            contentDescription = "Asistente AutoSmart",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(3.dp)
        )
    }
}

@Composable
private fun Burbuja(m: MensajeChat, onReservar: () -> Unit, onMisCitas: () -> Unit, onReintentar: () -> Unit) {
    val c = coloresCliente()
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.deUsuario) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!m.deUsuario) {
            AvatarAuto(30.dp)
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (m.deUsuario) 18.dp else 4.dp,
                bottomEnd = if (m.deUsuario) 4.dp else 18.dp
            ),
            color = when {
                m.deUsuario -> MarcaFuerte
                m.error -> c.rojoSuave
                else -> c.superficie
            },
            border = if (m.deUsuario) null else BorderStroke(1.dp, if (m.error) c.rojo.copy(alpha = 0.4f) else c.borde),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                m.imagen?.let {
                    Image(
                        it.asImageBitmap(), contentDescription = "Foto enviada", contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
                if (m.conIA) {
                    Surface(shape = RoundedCornerShape(8.dp), color = c.azulSuave) {
                        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = c.azul, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Respuesta con IA · referencial", color = c.azul, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text(
                    m.texto,
                    color = when {
                        m.deUsuario -> Color.White
                        m.error -> c.rojo
                        else -> c.texto
                    },
                    fontSize = 15.sp, lineHeight = 21.sp
                )
                m.tarjeta?.let { Tarjeta(it, onReservar = onReservar, onMisCitas = onMisCitas) }
                if (m.error) TextButton(onClick = onReintentar) { Text("Reintentar") }
            }
        }
    }
}

/** Tarjetas del chat: estado (T76.2), cotización (T77.1), agendar (T78.2) y asesor (T79.2). */
@Composable
private fun Tarjeta(t: TarjetaChat, onReservar: () -> Unit, onMisCitas: () -> Unit) {
    val c = coloresCliente()
    Surface(shape = RoundedCornerShape(14.dp), color = c.azulSuave, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (t) {
                is TarjetaChat.Estado -> {
                    FilaTarjeta(Icons.Outlined.DirectionsCar, "Ticket ${t.ticket ?: "-"} · ${t.placa ?: ""}")
                    Text((t.estado ?: "-").replace('_', ' '), color = c.azul, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    t.avance?.let { avance ->
                        LinearProgressIndicator(
                            progress = { (avance.coerceIn(0, 100)) / 100f },
                            color = MarcaFuerte, trackColor = c.borde,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Text("$avance % de avance", color = c.textoSecundario, fontSize = 12.sp)
                    }
                    TextButton(onClick = onMisCitas, contentPadding = PaddingValues(0.dp)) { Text("Ver seguimiento en Mis citas") }
                }
                is TarjetaChat.Cotizacion -> {
                    FilaTarjeta(Icons.Outlined.RequestQuote, t.servicio ?: "Servicio")
                    Text(rango(t.desde, t.hasta), color = c.texto, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFB45309).copy(alpha = 0.14f)) {
                        Text("Precio referencial: el costo final se confirma en el taller.", color = Color(0xFFB45309),
                            fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    TextButton(onClick = onReservar, contentPadding = PaddingValues(0.dp)) { Text("Reservar desde la app") }
                }
                is TarjetaChat.Resumen -> {
                    Text("Resumen de tu cita", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    FilaTarjeta(Icons.Outlined.Build, t.servicio ?: "-")
                    FilaTarjeta(Icons.Outlined.DirectionsCar, t.placa ?: "-")
                    FilaTarjeta(Icons.Outlined.CalendarMonth, listOfNotNull(t.fecha, t.hora).joinToString(" · "))
                    t.mecanico?.let { FilaTarjeta(Icons.Outlined.Person, it) }
                    Text("Toca Confirmar para registrar la cita o Cancelar para salir.", color = c.textoSecundario, fontSize = 12.sp)
                }
                is TarjetaChat.CitaCreada -> {
                    FilaTarjeta(Icons.Outlined.EventAvailable, "Cita ${t.ticket ?: ""} registrada")
                    Text("Estado: ${(t.estado ?: "PENDIENTE").replace('_', ' ')}", color = c.textoSecundario, fontSize = 13.sp)
                    TextButton(onClick = onMisCitas, contentPadding = PaddingValues(0.dp)) { Text(OPCION_MIS_CITAS) }
                }
                is TarjetaChat.Derivacion -> {
                    FilaTarjeta(Icons.Outlined.SupportAgent, "Solicitud N.° ${t.numero ?: "-"}")
                    Text("Un asesor del taller te contactará en horario de atención.", color = c.textoSecundario, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun FilaTarjeta(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    val c = coloresCliente()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = c.azul, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, color = c.texto, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun rango(desde: Double?, hasta: Double?): String {
    fun s(v: Double) = "S/ " + String.format(java.util.Locale.US, "%.2f", v)
    return when {
        desde != null && hasta != null && hasta > desde -> "${s(desde)} – ${s(hasta)}"
        desde != null -> s(desde)
        hasta != null -> s(hasta)
        else -> "Consultar en el taller"
    }
}

@Composable
private fun Escribiendo() {
    val c = coloresCliente()
    Row(verticalAlignment = Alignment.Bottom) {
        AvatarAuto(30.dp)
        Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = c.superficie, border = BorderStroke(1.dp, c.borde)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = c.azul, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("Escribiendo…", color = c.textoSecundario, fontSize = 14.sp)
            }
        }
    }
}

/** Lee la foto elegida reducida a 1024 px para no gastar memoria ni datos. */
private fun leerImagen(contexto: Context, uri: Uri): Bitmap? = runCatching {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
    var escala = 1
    while (maxOf(limites.outWidth, limites.outHeight) / (escala * 2) >= 1024) escala *= 2
    contexto.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = escala })
    }
}.getOrNull()
