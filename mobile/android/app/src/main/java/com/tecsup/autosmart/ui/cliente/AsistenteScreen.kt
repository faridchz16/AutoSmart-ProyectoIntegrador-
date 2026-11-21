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
import androidx.compose.material.icons.outlined.CalendarMonth
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
import com.tecsup.autosmart.viewmodel.OPCIONES_RAPIDAS
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
    onReservar: () -> Unit
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
                Burbuja(m, onReservar = onReservar, onReintentar = viewModel::reintentar)
            }
            if (estado.escribiendo) item(key = "escribiendo") { Escribiendo() }
        }

        // Opciones rápidas (T75.2)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            items(OPCIONES_RAPIDAS) { opcion ->
                SuggestionChip(
                    onClick = { viewModel.enviar(opcion) },
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
private fun Burbuja(m: MensajeChat, onReservar: () -> Unit, onReintentar: () -> Unit) {
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
                Text(
                    m.texto,
                    color = when {
                        m.deUsuario -> Color.White
                        m.error -> c.rojo
                        else -> c.texto
                    },
                    fontSize = 15.sp, lineHeight = 21.sp
                )
                if (m.reservar) {
                    Button(
                        onClick = onReservar, shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MarcaFuerte, contentColor = Color.White)
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reservar cita")
                    }
                }
                if (m.error) TextButton(onClick = onReintentar) { Text("Reintentar") }
            }
        }
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
