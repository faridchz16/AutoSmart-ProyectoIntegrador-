package com.tecsup.autosmart.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.tecsup.autosmart.data.repository.MAXIMO_BYTES_FOTO
import com.tecsup.autosmart.ui.viewmodel.FotosDeficienciaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class CapturarEvidencia : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent = super.createIntent(context, input).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        clipData = ClipData.newRawUri("Foto de deficiencia", input)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FotosDeficienciaScreen(idOrden: Long, idDeficiencia: Long, editable: Boolean,
                          viewModel: FotosDeficienciaViewModel, onBack: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    val context = LocalContext.current
    var confirmar by rememberSaveable { mutableStateOf(false) }
    val camara = rememberLauncherForActivityResult(CapturarEvidencia()) { viewModel.capturaTerminada(it) }
    val volver: () -> Unit = { if (!estado.subiendo && !estado.capturando) onBack() }
    BackHandler(onBack = volver)
    LaunchedEffect(idOrden, idDeficiencia) { viewModel.iniciar(idOrden, idDeficiencia, editable) }
    if (confirmar) AlertDialog(onDismissRequest = { confirmar = false }, title = { Text("¿Descartar la captura local?") },
        text = { Text("Las fotos guardadas en el servidor se conservarán. Si una subida quedó sin confirmar, revisa primero la lista del servidor.") },
        confirmButton = { TextButton(onClick = { confirmar = false; viewModel.descartarCaptura() }) { Text("Descartar captura") } },
        dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Conservar") } })
    Scaffold(topBar = {
        TopAppBar(title = { Text("Fotos de la deficiencia") }, navigationIcon = {
            IconButton(onClick = volver, enabled = !estado.subiendo && !estado.capturando) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a deficiencias")
            }
        }, actions = { TextButton(onClick = viewModel::cargar, enabled = !estado.ocupado) { Text("Actualizar") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Orden: $idOrden · Deficiencia: $idDeficiencia"); Text("JPG/PNG · Máximo 5 MB por archivo y 5 fotos.") }
            if (estado.cargando) item { CircularProgressIndicator() }
            estado.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
            estado.mensaje?.let { mensaje -> item { Text(mensaje) } }
            if (!estado.cargado && !estado.cargando) item { Button(onClick = viewModel::cargar) { Text("Reintentar consulta") } }
            if (estado.cargado) {
                if (!estado.editable) item { Text("Solo consulta: la orden no está en proceso.") }
                if (estado.editable) item {
                    Button(enabled = estado.puedeCapturar, onClick = {
                        try {
                            val carpeta = File(context.cacheDir, "fotos_deficiencias").apply { mkdirs() }
                            val file = File.createTempFile("evidencia_", ".jpg", carpeta)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fotos", file)
                            if (viewModel.prepararCamara(file)) {
                                try { camara.launch(uri) }
                                catch (e: Exception) { viewModel.capturaTerminada(false, "No se pudo abrir la cámara. Comprueba que haya una aplicación de cámara disponible.") }
                            }
                        } catch (e: Exception) { viewModel.informarError("No se pudo preparar la captura. Revisa el espacio disponible.") }
                    }) { Text(if (estado.capturando) "Cámara abierta..." else "Tomar foto") }
                    if (estado.fotos.size >= 5) Text("Esta deficiencia ya tiene el máximo de 5 fotos.")
                }
                estado.archivo?.let { path -> item {
                    Text("Captura local pendiente de envío", style = MaterialTheme.typography.titleMedium)
                    ImagenEvidencia(path, "Vista previa de la captura") {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val file = File(path)
                                require(file.isFile && file.length() in 1..MAXIMO_BYTES_FOTO) { "La captura ya no está disponible. Vuelve a tomarla." }
                                file.readBytes()
                            }
                        }
                    }
                    Button(onClick = viewModel::subir, enabled = estado.puedeCapturar) {
                        Text(if (estado.subiendo) "Enviando..." else "Enviar foto")
                    }
                    if (estado.verificar) Text("Subida sin confirmar. Actualiza y revisa las fotos del servidor antes de descartar esta captura; no se reenviará automáticamente.")
                    TextButton(onClick = { confirmar = true }, enabled = !estado.ocupado) { Text("Descartar captura local") }
                } }
                item { Text("Fotos guardadas: ${estado.fotos.size}/5", style = MaterialTheme.typography.titleMedium) }
                if (estado.fotos.isEmpty()) item { Text("Esta deficiencia aún no tiene fotos guardadas.") }
                items(estado.fotos, key = { it.idFoto }) { foto ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Foto ${foto.idFoto}")
                            ImagenEvidencia("servidor:${foto.idFoto}", "Foto ${foto.idFoto} de la deficiencia") { viewModel.obtener(foto.idFoto) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ImagenEvidencia(clave: String, descripcion: String, cargar: suspend () -> Result<ByteArray>) {
    var intento by remember(clave) { mutableIntStateOf(0) }
    val imagen by produceState<Result<Bitmap>?>(null, clave, intento) {
        value = null
        value = cargar().mapCatching { bytes -> withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "No se pudo leer la imagen." }
            var sample = 1
            while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
            requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample })) { "No se pudo mostrar la imagen." }
        } }
    }
    when {
        imagen == null -> CircularProgressIndicator(Modifier.padding(12.dp))
        imagen?.isSuccess == true -> Image(imagen!!.getOrThrow().asImageBitmap(), contentDescription = descripcion,
            modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Fit)
        else -> Column {
            Text(imagen?.exceptionOrNull()?.message ?: "No se pudo cargar la foto.", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { intento++ }) { Text("Reintentar imagen") }
        }
    }
}
