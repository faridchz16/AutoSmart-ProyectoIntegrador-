package com.tecsup.autosmart.ui.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tecsup.autosmart.model.VehiculoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Visor a pantalla completa de la foto del vehículo (guardada en el celular).
 * Se puede acercar con dos dedos y con doble toque. Si [onCambiar] es null, solo se visualiza.
 */
@Composable
fun VisorFotoVehiculo(
    idVehiculo: Long,
    titulo: String,
    subtitulo: String?,
    version: Int = 0,
    onCambiar: (() -> Unit)?,
    onCerrar: () -> Unit
) {
    val contexto = LocalContext.current
    val c = coloresCliente()
    // null = cargando; Result vacío = sin foto
    val foto by produceState<Result<ImageBitmap?>?>(initialValue = null, idVehiculo, version) {
        value = Result.success(
            withContext(Dispatchers.IO) { FotoVehiculoLocal.cargar(contexto, idVehiculo, maxLado = 1800)?.asImageBitmap() }
        )
    }
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }
    val transformable = rememberTransformableState { zoom, mover, _ ->
        escala = (escala * zoom).coerceIn(1f, 5f)
        desplazamiento = if (escala == 1f) Offset.Zero else desplazamiento + mover
    }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val actual = foto?.getOrNull()
            when {
                foto == null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
                actual != null -> Image(
                    bitmap = actual,
                    contentDescription = "Foto del vehículo $titulo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                if (escala > 1f) { escala = 1f; desplazamiento = Offset.Zero } else escala = 2.5f
                            })
                        }
                        .transformable(transformable)
                        .graphicsLayer(
                            scaleX = escala,
                            scaleY = escala,
                            translationX = desplazamiento.x,
                            translationY = desplazamiento.y
                        )
                )
                else -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.DirectionsCar, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(72.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Este vehículo aún no tiene foto", color = Color.White)
                }
            }

            // Barra superior: nombre del vehículo y cerrar
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    subtitulo?.let { Text(it, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall) }
                }
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            // Botón inferior para cambiar o agregar la foto
            if (onCambiar != null && foto != null) {
                Button(
                    onClick = { onCerrar(); onCambiar() },
                    colors = ButtonDefaults.buttonColors(containerColor = c.azul),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (actual != null) "Cambiar foto" else "Agregar foto")
                }
            }
        }
    }
}

/** Cabecera con la foto y los datos del vehículo elegido (pantalla de historial). */
@Composable
fun CabeceraVehiculoFoto(vehiculo: VehiculoResponse, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val c = coloresCliente()
    var verFoto by remember { mutableStateOf(false) }
    val foto by produceState<ImageBitmap?>(initialValue = null, vehiculo.idVehiculo) {
        value = withContext(Dispatchers.IO) { FotoVehiculoLocal.cargar(contexto, vehiculo.idVehiculo)?.asImageBitmap() }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = c.superficie),
        border = BorderStroke(1.dp, c.borde)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.azulSuave)
                    .clickable { verFoto = true },
                contentAlignment = Alignment.Center
            ) {
                val actual = foto
                if (actual != null) {
                    Image(actual, "Foto del vehículo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.DirectionsCar, null, tint = c.azul.copy(alpha = 0.55f), modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${vehiculo.marca} ${vehiculo.modelo}", fontWeight = FontWeight.Bold, color = c.texto,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(vehiculo.placa.uppercase(), color = c.azul, fontWeight = FontWeight.Bold)
                Text("Año ${vehiculo.anio}", color = c.textoSecundario, style = MaterialTheme.typography.bodySmall)
                if (foto != null) Text("Toca la foto para verla", color = c.textoSecundario, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (verFoto) {
        VisorFotoVehiculo(
            idVehiculo = vehiculo.idVehiculo,
            titulo = "${vehiculo.marca} ${vehiculo.modelo}",
            subtitulo = vehiculo.placa.uppercase(),
            onCambiar = null,
            onCerrar = { verFoto = false }
        )
    }
}
