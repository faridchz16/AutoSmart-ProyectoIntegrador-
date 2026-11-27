package com.tecsup.autosmart.ui.cliente

import android.app.Activity
import android.content.ContextWrapper
import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.autosmart.R
import com.tecsup.autosmart.data.repository.CitaRepository
import com.tecsup.autosmart.data.repository.PerfilRepository
import com.tecsup.autosmart.model.CitaCreadaResponse
import com.tecsup.autosmart.model.PerfilResponse
import com.tecsup.autosmart.model.VehiculoResponse
import com.tecsup.autosmart.ui.auth.fondoAuth
import com.tecsup.autosmart.viewmodel.VehiculosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Colores del sistema de diseño: cambian solos entre modo claro y oscuro (ColoresCliente.kt)
private val FondoApp: Color @Composable get() = coloresCliente().fondo
private val Superficie: Color @Composable get() = coloresCliente().superficie
private val Borde: Color @Composable get() = coloresCliente().borde
private val TextoPrincipal: Color @Composable get() = coloresCliente().texto
private val TextoSecundario: Color @Composable get() = coloresCliente().textoSecundario
private val AzulMarca: Color @Composable get() = coloresCliente().azul
private val AzulOscuro: Color @Composable get() = coloresCliente().azulOscuro
private val AzulSuave: Color @Composable get() = coloresCliente().azulSuave
private val Rojo: Color @Composable get() = coloresCliente().rojo
private val RojoSuave: Color @Composable get() = coloresCliente().rojoSuave

// Azules de marca para fondos llenos con texto blanco (iguales en claro y oscuro)
private val MarcaFuerte = Color(0xFF1150E8)
private val MarcaProfunda = Color(0xFF0A3AB0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiculosScreen(
    onLogout: () -> Unit,
    onEditarVehiculo: (VehiculoResponse) -> Unit = {},
    onNavigateToPerfil: () -> Unit = {},
    onNavigateToMisCitas: () -> Unit = {},
    onNavigateToReserva: () -> Unit = {},
    onNavigateToAgregarVehiculo: () -> Unit = {},
    onNavigateToAsistente: () -> Unit = {},
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

    val configuracion = LocalConfiguration.current
    val horizontal = configuracion.orientation == Configuration.ORIENTATION_LANDSCAPE
    val angosto = configuracion.screenWidthDp < 400

    // Perfil para el avatar con la inicial del correo
    var perfil by remember { mutableStateOf<PerfilResponse?>(null) }
    LaunchedEffect(Unit) { perfil = PerfilRepository().obtenerPerfil().getOrNull() }

    // Notificaciones (campanita): avisos armados con el estado de las citas del cliente
    var citasAvisos by remember { mutableStateOf<List<CitaCreadaResponse>>(emptyList()) }
    var cargandoAvisos by remember { mutableStateOf(false) }
    var recargarAvisos by remember { mutableIntStateOf(0) }
    var verAvisos by remember { mutableStateOf(false) }
    var leidosAvisos by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(recargarAvisos) {
        cargandoAvisos = true
        CitaRepository().misCitas().onSuccess { citasAvisos = it }
        cargandoAvisos = false
    }
    LaunchedEffect(perfil?.idUsuario) {
        perfil?.idUsuario?.let { leidosAvisos = AvisosLeidos.leidos(contexto, it) }
    }
    val avisos = remember(citasAvisos) { avisosDe(citasAvisos) }
    val avisosNuevos = avisos.count { it.clave !in leidosAvisos }
    val marcarAvisosLeidos = {
        perfil?.idUsuario?.let { id ->
            AvisosLeidos.marcar(contexto, id, avisos.map { it.clave })
            leidosAvisos = AvisosLeidos.leidos(contexto, id)
        }
    }
    if (verAvisos) {
        PanelAvisos(
            avisos = avisos,
            leidos = leidosAvisos,
            cargando = cargandoAvisos,
            onAbrir = { marcarAvisosLeidos(); verAvisos = false; onNavigateToMisCitas() },
            onCerrar = { marcarAvisosLeidos(); verAvisos = false }
        )
    }

    // Anuncio de ARI: se muestra solo la primera vez que cada cliente entra al inicio
    var mostrarAnuncioAri by remember { mutableStateOf(false) }
    LaunchedEffect(perfil?.idUsuario) {
        val id = perfil?.idUsuario ?: return@LaunchedEffect
        if (!AnuncioAri.visto(contexto, id)) mostrarAnuncioAri = true
    }
    perfil?.idUsuario?.let { id ->
        if (mostrarAnuncioAri) {
            AnuncioAriDialog(
                onProbar = {
                    AnuncioAri.marcarVisto(contexto, id)
                    mostrarAnuncioAri = false
                    onNavigateToAsistente()
                },
                onCerrar = {
                    AnuncioAri.marcarVisto(contexto, id)
                    mostrarAnuncioAri = false
                }
            )
        }
    }

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
    // Vehículo cuya foto se está viendo a pantalla completa
    var fotoAbierta by remember { mutableStateOf<VehiculoResponse?>(null) }
    fotoAbierta?.let { v ->
        VisorFotoVehiculo(
            idVehiculo = v.idVehiculo,
            titulo = "${v.marca} ${v.modelo}",
            subtitulo = v.placa.uppercase(),
            version = versionFotos,
            onCambiar = { pedirFoto(v.idVehiculo) },
            onCerrar = { fotoAbierta = null }
        )
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

    val habilitado = !estado.ocupado
    // Destinos del menú inferior (vertical) o lateral (horizontal)
    val destinos = listOf(
        Destino("Inicio", Icons.Filled.Home, seleccionado = true) {},
        // El catálogo de servicios sigue en la pestaña "Servicios" de arriba; aquí va el asistente con IA
        Destino("ARI", Icons.Outlined.SmartToy, imagen = R.drawable.img_ari_figura) { if (habilitado) onNavigateToAsistente() },
        Destino("Reservar", Icons.Outlined.EventAvailable) { if (habilitado) onNavigateToReserva() },
        Destino("Perfil", Icons.Outlined.Person) { if (habilitado) onNavigateToPerfil() }
    )

    val contenido: @Composable (Modifier) -> Unit = { mod ->
        LazyVerticalGrid(
            columns = if (horizontal) GridCells.Adaptive(330.dp) else GridCells.Fixed(1),
            modifier = mod.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            filaCompleta("encabezado") {
                Column {
                    EncabezadoCliente(
                        perfil = perfil,
                        horizontal = horizontal,
                        habilitado = habilitado,
                        onPerfil = onNavigateToPerfil,
                        onSalir = onLogout,
                        avisosNuevos = avisosNuevos,
                        onAvisos = { recargarAvisos++; verAvisos = true }
                    )
                    PestanasCliente(
                        angosto = angosto,
                        habilitado = habilitado,
                        onMisCitas = onNavigateToMisCitas,
                        onServicios = onNavigateToReserva,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .offset(y = (-30).dp)
                    )
                }
            }

            when {
                estado.cargando -> filaCompleta("cargando") {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AzulMarca)
                    }
                }

                estado.errorCarga != null -> filaCompleta("error") {
                    MensajeCentro(
                        titulo = "No pudimos cargar tus vehículos",
                        detalle = estado.errorCarga.orEmpty(),
                        textoBoton = "Reintentar",
                        onBoton = viewModel::cargar
                    )
                }

                vehiculos.isEmpty() -> filaCompleta("vacio") {
                    MensajeCentro(
                        titulo = "Aún no tienes vehículos",
                        detalle = "Agrega tu vehículo para reservar mantenimientos y ver su historial.",
                        textoBoton = "Agregar vehículo",
                        onBoton = onNavigateToAgregarVehiculo
                    )
                }

                else -> {
                    filaCompleta("titulo") {
                        TituloSeccion(
                            cantidad = vehiculos.size,
                            angosto = angosto,
                            habilitado = habilitado,
                            onAgregar = onNavigateToAgregarVehiculo,
                            modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-14).dp)
                        )
                    }
                    estado.errorOperacion?.let { mensaje ->
                        filaCompleta("errorOperacion") {
                            Column(Modifier.padding(horizontal = 20.dp)) {
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
                        filaCompleta("mensaje") {
                            Text(mensaje, color = TextoSecundario, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }
                    if (estado.ocupado && estado.seleccionado == null) {
                        filaCompleta("progreso") {
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = AzulMarca)
                        }
                    }
                    items(vehiculos, key = { it.idVehiculo }) { vehiculo ->
                        val accionesActivas = !estado.ocupado && estado.pendienteVerificacion == null
                        TarjetaVehiculo(
                            vehiculo = vehiculo,
                            versionFotos = versionFotos,
                            angosto = angosto,
                            accionesActivas = accionesActivas,
                            onFoto = { pedirFoto(vehiculo.idVehiculo) },
                            onVerFoto = { fotoAbierta = vehiculo },
                            onHistorial = { onHistorialVehiculo(vehiculo.idVehiculo) },
                            onEditar = { onEditarVehiculo(vehiculo) },
                            onEliminar = { viewModel.seleccionar(vehiculo) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    if (horizontal) {
        // Horizontal: menú lateral para no robar alto a la lista
        Row(Modifier.fillMaxSize().background(FondoApp)) {
            NavigationRail(
                containerColor = Superficie,
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(Modifier.weight(1f))
                destinos.forEach { d ->
                    NavigationRailItem(
                        selected = d.seleccionado,
                        onClick = d.onClick,
                        icon = { IconoDestino(d) },
                        label = { Text(d.texto, fontSize = 12.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = AzulMarca,
                            selectedTextColor = AzulMarca,
                            indicatorColor = AzulSuave,
                            unselectedIconColor = TextoPrincipal,
                            unselectedTextColor = TextoPrincipal
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            contenido(Modifier.weight(1f))
        }
    } else {
        Scaffold(
            containerColor = FondoApp,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(
                    containerColor = Superficie,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                ) {
                    destinos.forEach { d ->
                        NavigationBarItem(
                            selected = d.seleccionado,
                            onClick = d.onClick,
                            icon = { IconoDestino(d) },
                            label = { Text(d.texto, fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AzulMarca,
                                selectedTextColor = AzulMarca,
                                indicatorColor = AzulSuave,
                                unselectedIconColor = TextoPrincipal,
                                unselectedTextColor = TextoPrincipal
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            contenido(Modifier.padding(innerPadding))
        }
    }
}

private class Destino(
    val texto: String,
    val icono: ImageVector,
    val seleccionado: Boolean = false,
    val imagen: Int? = null, // si tiene imagen (ARI) se muestra en vez del ícono
    val onClick: () -> Unit
)

/** Ícono del menú: la figura de ARI o el ícono normal. */
@Composable
private fun IconoDestino(d: Destino) {
    val imagen = d.imagen
    if (imagen != null) {
        // Figura completa de ARI (sin recortar en círculo) para que se reconozca
        Image(
            painterResource(imagen),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 46.dp, height = 30.dp)
        )
    } else {
        Icon(d.icono, contentDescription = null)
    }
}

/** Ítem que ocupa todo el ancho de la grilla (encabezado, títulos, mensajes). */
private fun LazyGridScope.filaCompleta(clave: String, contenido: @Composable () -> Unit) {
    item(key = clave, span = { GridItemSpan(maxLineSpan) }) { contenido() }
}

/** Encabezado azul con logo, marca, auto de fondo y avatar con la inicial del correo. */
@Composable
private fun EncabezadoCliente(
    perfil: PerfilResponse?,
    horizontal: Boolean,
    habilitado: Boolean,
    onPerfil: () -> Unit,
    onSalir: () -> Unit,
    avisosNuevos: Int = 0,
    onAvisos: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .fondoAuth()
    ) {
        // Auto de la portada, tenue, al fondo a la derecha
        Image(
            painter = painterResource(R.drawable.img_welcome_auto),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = if (horizontal) 80.dp else 0.dp)
                .height(if (horizontal) 110.dp else 120.dp)
                .offset(x = if (horizontal) 0.dp else 40.dp, y = 6.dp)
                .alpha(0.30f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = 18.dp, end = 16.dp,
                top = if (horizontal) 14.dp else 22.dp,
                bottom = if (horizontal) 46.dp else 56.dp
            )
        ) {
            Image(
                painter = painterResource(R.drawable.ic_autosmart_logo),
                contentDescription = "AutoSmart",
                modifier = Modifier.size(if (horizontal) 46.dp else 56.dp)
            )
            Box(
                Modifier
                    .padding(horizontal = 10.dp)
                    .width(1.5.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.7f))
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "AutoSmart",
                    color = Color.White,
                    fontSize = if (horizontal) 24.sp else 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1
                )
                Text(
                    "Gestión de vehículos y servicios",
                    color = Color(0xFFDCE6F8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Campanita de notificaciones con el número de avisos nuevos
            CampanaAvisos(nuevos = avisosNuevos, habilitado = habilitado, onClick = onAvisos)
            Spacer(Modifier.width(4.dp))
            MenuAvatar(perfil = perfil, habilitado = habilitado, onPerfil = onPerfil, onSalir = onSalir)
        }
    }
}

/** Avatar con la inicial; al tocarlo muestra "Mi perfil" y "Cerrar sesión". */
@Composable
private fun MenuAvatar(perfil: PerfilResponse?, habilitado: Boolean, onPerfil: () -> Unit, onSalir: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        AvatarInicial(
            idUsuario = perfil?.idUsuario,
            correo = perfil?.correo,
            nombre = perfil?.nombre,
            habilitado = habilitado,
            onClick = { abierto = true }
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            val nombre = perfil?.nombre?.takeIf { it.isNotBlank() }
            if (nombre != null || perfil?.correo != null) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    nombre?.let { Text(it, fontWeight = FontWeight.Bold, color = TextoPrincipal) }
                    perfil?.correo?.let { Text(it, fontSize = 12.sp, color = TextoSecundario) }
                }
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Mi perfil") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                onClick = { abierto = false; onPerfil() }
            )
            DropdownMenuItem(
                text = { Text("Cerrar sesión", color = Rojo) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Rojo) },
                onClick = { abierto = false; onSalir() }
            )
        }
    }
}

/** Foto de perfil si el usuario la eligió; si no, círculo con la inicial del correo (como Gmail). */
@Composable
private fun AvatarInicial(idUsuario: Int?, correo: String?, nombre: String?, habilitado: Boolean, onClick: () -> Unit) {
    val contexto = LocalContext.current
    val foto by produceState<ImageBitmap?>(initialValue = null, idUsuario) {
        value = idUsuario?.let { id ->
            withContext(Dispatchers.IO) { FotoVehiculoLocal.cargarPerfil(contexto, id)?.asImageBitmap() }
        }
    }
    val base = correo?.trim()?.takeIf { it.isNotEmpty() } ?: nombre?.trim()
    val letra = base?.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
    val paleta = listOf(
        0xFF1A73E8, 0xFFD93025, 0xFF188038, 0xFFE37400,
        0xFF9334E6, 0xFF00897B, 0xFFC2185B, 0xFF3949AB
    )
    val color = Color(paleta[(base?.lowercase()?.hashCode() ?: 0).mod(paleta.size)])
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (letra != null) color else Color.White.copy(alpha = 0.2f))
            .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            .clickable(enabled = habilitado, onClick = onClick)
            .semantics { contentDescription = "Menú de cuenta" },
        contentAlignment = Alignment.Center
    ) {
        val actual = foto
        if (actual != null) {
            Image(actual, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        } else if (letra != null) {
            Text(letra, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
        }
    }
}

/** Tarjeta blanca con las tres pestañas, montada sobre el borde del encabezado. */
@Composable
private fun PestanasCliente(
    angosto: Boolean,
    habilitado: Boolean,
    onMisCitas: () -> Unit,
    onServicios: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Superficie,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pestana("Mis vehículos", Icons.Outlined.DirectionsCar, true, angosto, true, Modifier.weight(1.25f)) {}
            Pestana("Mis citas", Icons.Outlined.EventAvailable, false, angosto, habilitado, Modifier.weight(1f), onMisCitas)
            Pestana("Servicios", Icons.Outlined.Build, false, angosto, habilitado, Modifier.weight(1f), onServicios)
        }
    }
}

@Composable
private fun Pestana(
    texto: String,
    icono: ImageVector,
    seleccionada: Boolean,
    angosto: Boolean,
    habilitado: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val fondo = if (seleccionada) Brush.horizontalGradient(listOf(MarcaProfunda, MarcaFuerte))
    else Brush.horizontalGradient(listOf(AzulSuave, AzulSuave))
    val color = if (seleccionada) Color.White else TextoPrincipal
    Box(
        modifier = modifier
            .height(if (angosto) 46.dp else 50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(fondo)
            .clickable(enabled = habilitado && !seleccionada, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(if (angosto) 17.dp else 20.dp))
            Spacer(Modifier.width(if (angosto) 4.dp else 8.dp))
            Text(
                texto,
                color = color,
                fontSize = if (angosto) 12.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** "Mis vehículos registrados (n)" con el botón "Agregar". */
@Composable
private fun TituloSeccion(cantidad: Int, angosto: Boolean, habilitado: Boolean, onAgregar: () -> Unit, modifier: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Text(
            buildAnnotatedString {
                append("Mis vehículos registrados ")
                withStyle(SpanStyle(color = AzulMarca)) { append("($cantidad)") }
            },
            fontSize = if (angosto) 17.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onAgregar,
            enabled = habilitado,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MarcaFuerte, contentColor = Color.White),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Agregar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun TarjetaVehiculo(
    vehiculo: VehiculoResponse,
    versionFotos: Int,
    angosto: Boolean,
    accionesActivas: Boolean,
    onFoto: () -> Unit,
    onVerFoto: () -> Unit,
    onHistorial: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        border = BorderStroke(1.dp, Borde),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row {
                FotoVehiculo(
                    idVehiculo = vehiculo.idVehiculo,
                    version = versionFotos,
                    tamano = if (angosto) 112.dp else 128.dp,
                    onVer = onVerFoto,
                    onCambiar = onFoto
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            "${vehiculo.marca} ${vehiculo.modelo}",
                            fontSize = if (angosto) 17.sp else 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(top = 2.dp)
                        )
                        Box {
                            IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", tint = TextoPrincipal)
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Ver foto") },
                                    leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                                    onClick = { menu = false; onVerFoto() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cambiar foto") },
                                    leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                                    onClick = { menu = false; onFoto() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ver historial") },
                                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                                    onClick = { menu = false; onHistorial() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Editar") },
                                    enabled = accionesActivas,
                                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                    onClick = { menu = false; onEditar() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar", color = if (accionesActivas) Rojo else Rojo.copy(alpha = 0.4f)) },
                                    enabled = accionesActivas,
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Rojo) },
                                    onClick = { menu = false; onEliminar() }
                                )
                            }
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = AzulSuave) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = AzulMarca, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                vehiculo.placa.uppercase(),
                                color = AzulMarca,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    DatoLinea(Icons.Outlined.CalendarToday, "Año: ${vehiculo.anio}")
                    DatoLinea(
                        Icons.Outlined.Speed,
                        (if (angosto) "" else "Kilometraje: ") + String.format(Locale("es", "PE"), "%,d km", vehiculo.kilometraje)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonAccion(
                    texto = if (angosto) "Historial" else "Ver historial",
                    icono = Icons.Outlined.Description,
                    habilitado = true,
                    peligro = false,
                    angosto = angosto,
                    modifier = Modifier.weight(1.15f),
                    onClick = onHistorial
                )
                BotonAccion("Editar", Icons.Outlined.Edit, accionesActivas, false, angosto, Modifier.weight(1f), onEditar)
                BotonAccion("Eliminar", Icons.Outlined.DeleteOutline, accionesActivas, true, angosto, Modifier.weight(1f), onEliminar)
            }
        }
    }
}

@Composable
private fun DatoLinea(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = TextoPrincipal, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 14.sp, color = TextoPrincipal, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BotonAccion(
    texto: String,
    icono: ImageVector,
    habilitado: Boolean,
    peligro: Boolean,
    angosto: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val color = if (peligro) Rojo else AzulOscuro
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        color = if (peligro) RojoSuave else AzulSuave,
        border = if (peligro) BorderStroke(1.dp, Rojo.copy(alpha = 0.45f)) else null,
        modifier = modifier.height(44.dp).alpha(if (habilitado) 1f else 0.45f)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(if (angosto) 16.dp else 18.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                texto,
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (angosto) 12.sp else 14.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** Foto del vehículo (guardada en el celular) o un dibujo si aún no tiene. */
@Composable
private fun FotoVehiculo(idVehiculo: Long, version: Int, tamano: Dp, onVer: () -> Unit, onCambiar: () -> Unit) {
    val contexto = LocalContext.current
    val foto by produceState<ImageBitmap?>(initialValue = null, idVehiculo, version) {
        value = withContext(Dispatchers.IO) { FotoVehiculoLocal.cargar(contexto, idVehiculo)?.asImageBitmap() }
    }
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(RoundedCornerShape(18.dp))
            // Con foto: se abre para verla; sin foto: se elige una
            .clickable { if (foto != null) onVer() else onCambiar() }
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
                    .background(Brush.linearGradient(listOf(AzulSuave, Borde))),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = AzulMarca.copy(alpha = 0.55f),
                    modifier = Modifier.size(44.dp)
                )
                Text("Agregar foto", color = TextoSecundario, fontSize = 12.sp)
            }
        }
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onCambiar)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = if (actual != null) "Cambiar foto" else "Agregar foto",
                    tint = AzulMarca,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** Estado vacío o de error, centrado. */
@Composable
private fun MensajeCentro(titulo: String, detalle: String, textoBoton: String, onBoton: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(88.dp).clip(CircleShape).background(AzulSuave),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = AzulMarca, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(detalle, fontSize = 14.sp, color = TextoSecundario, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onBoton,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MarcaFuerte, contentColor = Color.White)
        ) { Text(textoBoton, fontWeight = FontWeight.SemiBold) }
    }
}
