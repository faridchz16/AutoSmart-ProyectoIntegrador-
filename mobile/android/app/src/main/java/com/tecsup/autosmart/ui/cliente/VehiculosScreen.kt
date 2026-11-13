package com.tecsup.autosmart.ui.cliente

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.autosmart.data.repository.PerfilRepository
import com.tecsup.autosmart.model.PerfilResponse
import com.tecsup.autosmart.model.VehiculoResponse
import com.tecsup.autosmart.ui.auth.fondoAuth
import com.tecsup.autosmart.viewmodel.VehiculosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Colores del sistema de diseño (página "Sistema de diseño" en Figma)
private val FondoApp = Color(0xFFF5F8FE)
private val Superficie = Color.White
private val Borde = Color(0xFFD5DCEA)
private val TextoPrincipal = Color(0xFF0B1B3A)
private val TextoSecundario = Color(0xFF5A677F)
private val AzulMarca = Color(0xFF1150E8)
private val AzulSuave = Color(0xFFE9F0FF)
private val Rojo = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiculosScreen(
    onLogout: () -> Unit,
    onEditarVehiculo: (VehiculoResponse) -> Unit = {},
    onNavigateToPerfil: () -> Unit = {},
    onNavigateToMisCitas: () -> Unit = {},
    onNavigateToReserva: () -> Unit = {},
    onNavigateToAgregarVehiculo: () -> Unit = {},
    viewModel: VehiculosViewModel = viewModel(),
    onHistorialVehiculo: (Long) -> Unit = {}
) {
    val estado by viewModel.estado.collectAsState()
    val vehiculos = estado.vehiculos
    val contexto = LocalContext.current
    val actividad = remember(contexto) {
        generateSequence(contexto) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
    }
    LaunchedEffect(viewModel) { viewModel.cargarSiNecesario() }
    DisposableEffect(viewModel, actividad) {
        onDispose { if (actividad?.isChangingConfigurations != true) viewModel.limpiar() }
    }
    BackHandler(enabled = estado.ocupado) { }

    // Perfil para el saludo y el avatar con la inicial del correo
    var perfil by remember { mutableStateOf<PerfilResponse?>(null) }
    LaunchedEffect(Unit) { perfil = PerfilRepository().obtenerPerfil().getOrNull() }

    // Foto del vehículo elegida desde la galería (se guarda en el celular)
    val scope = rememberCoroutineScope()
    var vehiculoParaFoto by rememberSaveable { mutableStateOf<Long?>(null) }
    var versionFotos by remember { mutableIntStateOf(0) }
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = vehiculoParaFoto
        if (uri != null && id != null) {
            scope.launch {
                withContext(Dispatchers.IO) { FotoVehiculoLocal.guardar(contexto, id, uri) }
                versionFotos++
            }
        }
        vehiculoParaFoto = null
    }
    val pedirFoto: (Long) -> Unit = { id ->
        vehiculoParaFoto = id
        elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    estado.seleccionado?.let { vehiculo ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarConfirmacion,
            title = { Text("Eliminar vehículo") },
            text = {
                Text("¿Deseas eliminar el vehículo ${vehiculo.placa} de tu lista? Esta acción no cancela tus citas existentes.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarEliminacion, enabled = !estado.ocupado) {
                    Text(if (estado.ocupado) "Eliminando..." else "Sí, eliminar", color = Rojo)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cerrarConfirmacion, enabled = !estado.ocupado) {
                    Text("Conservar")
                }
            }
        )
    }

    Scaffold(
        containerColor = FondoApp,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!estado.ocupado) onNavigateToAgregarVehiculo() },
                containerColor = AzulMarca,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Agregar vehículo", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EncabezadoCliente(
                perfil = perfil,
                habilitado = !estado.ocupado,
                onPerfil = onNavigateToPerfil,
                onReservar = onNavigateToReserva,
                onMisCitas = onNavigateToMisCitas,
                onSalir = onLogout
            )

            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AzulMarca)
                }

                estado.errorCarga != null -> MensajeCentro(
                    titulo = "No pudimos cargar tus vehículos",
                    detalle = estado.errorCarga.orEmpty(),
                    textoBoton = "Reintentar",
                    onBoton = viewModel::cargar
                )

                vehiculos.isEmpty() -> MensajeCentro(
                    titulo = "Aún no tienes vehículos",
                    detalle = "Agrega tu vehículo para reservar mantenimientos y ver su historial.",
                    textoBoton = "Agregar vehículo",
                    onBoton = onNavigateToAgregarVehiculo
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Mis vehículos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoPrincipal,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(shape = CircleShape, color = AzulSuave) {
                                Text(
                                    "${vehiculos.size}",
                                    color = AzulMarca,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    estado.errorOperacion?.let { mensaje ->
                        item {
                            Column {
                                Text(mensaje, color = Rojo, fontSize = 14.sp)
                                if (estado.pendienteVerificacion != null) {
                                    TextButton(onClick = viewModel::comprobarEstado, enabled = !estado.ocupado) {
                                        Text(if (estado.ocupado) "Comprobando..." else "Comprobar estado")
                                    }
                                }
                            }
                        }
                    }
                    estado.mensaje?.let { mensaje ->
                        item { Text(mensaje, color = TextoSecundario, fontSize = 14.sp) }
                    }
                    if (estado.ocupado && estado.seleccionado == null) {
                        item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = AzulMarca) }
                    }
                    items(vehiculos, key = { it.idVehiculo }) { vehiculo ->
                        val accionesActivas = !estado.ocupado && estado.pendienteVerificacion == null
                        TarjetaVehiculo(
                            vehiculo = vehiculo,
                            versionFotos = versionFotos,
                            accionesActivas = accionesActivas,
                            onFoto = { pedirFoto(vehiculo.idVehiculo) },
                            onHistorial = { onHistorialVehiculo(vehiculo.idVehiculo) },
                            onEditar = { onEditarVehiculo(vehiculo) },
                            onEliminar = { viewModel.seleccionar(vehiculo) }
                        )
                    }
                }
            }
        }
    }
}

/** Encabezado azul con saludo, avatar con inicial y accesos rápidos. */
@Composable
private fun EncabezadoCliente(
    perfil: PerfilResponse?,
    habilitado: Boolean,
    onPerfil: () -> Unit,
    onReservar: () -> Unit,
    onMisCitas: () -> Unit,
    onSalir: () -> Unit
) {
    val primerNombre = perfil?.nombre?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .fondoAuth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 22.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (primerNombre != null) "Hola, $primerNombre" else "Hola",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Tus vehículos y mantenimientos",
                        color = Color(0xFFDCE6F8),
                        fontSize = 14.sp
                    )
                }
                IconButton(onClick = onSalir, enabled = habilitado) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                AvatarInicial(
                    correo = perfil?.correo,
                    nombre = perfil?.nombre,
                    habilitado = habilitado,
                    onClick = onPerfil
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onReservar,
                    enabled = habilitado,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AzulMarca)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reservar cita", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = onMisCitas,
                    enabled = habilitado,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mis citas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/** Círculo con la inicial del correo, con un color fijo por usuario (como Gmail). */
@Composable
private fun AvatarInicial(correo: String?, nombre: String?, habilitado: Boolean, onClick: () -> Unit) {
    val base = correo?.trim()?.takeIf { it.isNotEmpty() } ?: nombre?.trim()
    val letra = base?.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
    val paleta = listOf(
        0xFF1A73E8, 0xFFD93025, 0xFF188038, 0xFFE37400,
        0xFF9334E6, 0xFF00897B, 0xFFC2185B, 0xFF3949AB
    )
    val color = Color(paleta[(base?.lowercase()?.hashCode() ?: 0).mod(paleta.size)])
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (letra != null) color else Color.White.copy(alpha = 0.2f))
            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .clickable(enabled = habilitado, onClick = onClick)
            .semantics { contentDescription = "Mi perfil" },
        contentAlignment = Alignment.Center
    ) {
        if (letra != null) {
            Text(letra, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun TarjetaVehiculo(
    vehiculo: VehiculoResponse,
    versionFotos: Int,
    accionesActivas: Boolean,
    onFoto: () -> Unit,
    onHistorial: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        border = BorderStroke(1.dp, Borde),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        FotoVehiculo(idVehiculo = vehiculo.idVehiculo, version = versionFotos, onCambiar = onFoto)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${vehiculo.marca} ${vehiculo.modelo}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Modelo ${vehiculo.anio}", fontSize = 13.sp, color = TextoSecundario)
                }
                Placa(vehiculo.placa)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatoChip(Icons.Default.Speed, String.format(Locale("es", "PE"), "%,d km", vehiculo.kilometraje))
                DatoChip(Icons.Default.CalendarToday, vehiculo.anio.toString())
            }
            HorizontalDivider(color = Color(0xFFE5EAF3))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccionTexto(Icons.Default.History, "Historial", AzulMarca, true, onHistorial)
                AccionTexto(Icons.Default.Edit, "Editar", AzulMarca, accionesActivas, onEditar)
                Spacer(Modifier.weight(1f))
                AccionTexto(Icons.Default.DeleteOutline, "Eliminar", Rojo, accionesActivas, onEliminar)
            }
        }
    }
}

/** Foto del vehículo (guardada en el celular) o un dibujo si aún no tiene. */
@Composable
private fun FotoVehiculo(idVehiculo: Long, version: Int, onCambiar: () -> Unit) {
    val contexto = LocalContext.current
    val foto by produceState<ImageBitmap?>(initialValue = null, idVehiculo, version) {
        value = withContext(Dispatchers.IO) { FotoVehiculoLocal.cargar(contexto, idVehiculo)?.asImageBitmap() }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
    ) {
        val actual = foto
        if (actual != null) {
            Image(
                bitmap = actual,
                contentDescription = "Foto del vehículo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(listOf(AzulSuave, Color(0xFFDBE7FF))))
                    .clickable(onClick = onCambiar),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = AzulMarca.copy(alpha = 0.55f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("Agrega una foto de tu vehículo", color = TextoSecundario, fontSize = 13.sp)
            }
        }
        Surface(
            onClick = onCambiar,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = if (actual != null) "Cambiar foto" else "Agregar foto",
                    tint = AzulMarca,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Placa con estilo de placa de auto. */
@Composable
private fun Placa(placa: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, TextoPrincipal)
    ) {
        Text(
            placa.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            color = TextoPrincipal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DatoChip(icono: ImageVector, texto: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = FondoApp, border = BorderStroke(1.dp, Borde)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(icono, contentDescription = null, tint = TextoSecundario, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(texto, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextoPrincipal)
        }
    }
}

@Composable
private fun AccionTexto(icono: ImageVector, texto: String, color: Color, habilitado: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = habilitado, contentPadding = PaddingValues(horizontal = 10.dp)) {
        Icon(icono, contentDescription = null, tint = if (habilitado) color else color.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(texto, color = if (habilitado) color else color.copy(alpha = 0.4f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** Estado vacío o de error, centrado. */
@Composable
private fun MensajeCentro(titulo: String, detalle: String, textoBoton: String, onBoton: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(AzulSuave),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = AzulMarca, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(detalle, fontSize = 14.sp, color = TextoSecundario, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onBoton,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AzulMarca)
        ) { Text(textoBoton, fontWeight = FontWeight.SemiBold) }
    }
}
