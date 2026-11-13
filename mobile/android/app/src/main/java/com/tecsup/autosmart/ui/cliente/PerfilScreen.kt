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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.autosmart.BuildConfig
import com.tecsup.autosmart.R
import com.tecsup.autosmart.model.PerfilResponse
import com.tecsup.autosmart.ui.auth.fondoAuth
import com.tecsup.autosmart.ui.theme.Apariencia
import com.tecsup.autosmart.viewmodel.PerfilViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Azules de marca para fondos llenos con texto blanco (iguales en claro y oscuro)
private val MarcaFuerte = Color(0xFF1150E8)
private val Exito = Color(0xFF16A34A)

@Composable
fun PerfilScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onCambiarContrasena: (correo: String) -> Unit = {},
    viewModel: PerfilViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    BackHandler(enabled = estado.guardando || estado.editando) {
        if (!estado.guardando) viewModel.cancelarEdicion()
    }
    val contexto = LocalContext.current
    val actividad = remember(contexto) {
        generateSequence(contexto) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
    }
    LaunchedEffect(viewModel) { viewModel.cargarSiNecesario() }
    DisposableEffect(viewModel, actividad) {
        onDispose {
            if (actividad?.isChangingConfigurations != true) viewModel.limpiar()
        }
    }

    val c = coloresCliente()
    val configuracion = LocalConfiguration.current
    val horizontal = configuracion.orientation == Configuration.ORIENTATION_LANDSCAPE
    // En horizontal hay dos columnas, así que cada tarjeta usa el diseño compacto si cada mitad es angosta
    val angosto = if (horizontal) configuracion.screenWidthDp < 820 else configuracion.screenWidthDp < 400

    var verAcercaDe by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }

    // Foto de perfil elegida desde la galería (se guarda en el celular)
    val scope = rememberCoroutineScope()
    var versionFoto by remember { mutableIntStateOf(0) }
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = estado.perfil?.idUsuario
        if (uri != null && id != null) {
            scope.launch {
                withContext(Dispatchers.IO) { FotoVehiculoLocal.guardarPerfil(contexto, id, uri) }
                versionFoto++
            }
        }
    }
    val pedirFoto = {
        elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    if (verAcercaDe) {
        AlertDialog(
            onDismissRequest = { verAcercaDe = false },
            icon = {
                Image(
                    painterResource(R.drawable.ic_autosmart_logo),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(MarcaFuerte).padding(6.dp)
                )
            },
            title = { Text("AutoSmart") },
            text = {
                Text("Versión ${BuildConfig.VERSION_NAME}\n\nGestiona tus vehículos, reserva mantenimientos y sigue el avance de tus servicios en el taller.")
            },
            confirmButton = { TextButton(onClick = { verAcercaDe = false }) { Text("Entendido") } }
        )
    }
    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres salir de tu cuenta?") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; onLogout() }) {
                    Text("Cerrar sesión", color = c.rojo)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.fondo)
            .verticalScroll(rememberScrollState())
    ) {
        EncabezadoPerfil(
            horizontal = horizontal,
            puedeActualizar = !estado.cargando && !estado.editando && !estado.guardando,
            puedeVolver = !estado.guardando,
            onVolver = { if (estado.editando) viewModel.cancelarEdicion() else onNavigateBack() },
            onActualizar = { viewModel.cargarPerfil() }
        )

        val solape = if (horizontal) 44.dp else 56.dp
        val contenidoMod = Modifier
            .padding(horizontal = 16.dp)
            .offset(y = -solape)

        val perfil = estado.perfil
        when {
            estado.cargando -> TarjetaBase(contenidoMod) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = c.azul)
                    Spacer(Modifier.height(12.dp))
                    Text("Cargando tu perfil...", color = c.textoSecundario)
                }
            }

            estado.error != null || perfil == null -> TarjetaBase(contenidoMod) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(estado.error ?: "No se pudo cargar tu perfil.", color = c.rojo)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.cargarPerfil() },
                        colors = ButtonDefaults.buttonColors(containerColor = MarcaFuerte, contentColor = Color.White)
                    ) { Text("Reintentar") }
                }
            }

            else -> {
                val tarjetaPerfil: @Composable (Modifier) -> Unit = { mod ->
                    TarjetaPerfil(
                        perfil = perfil,
                        editando = estado.editando,
                        guardando = estado.guardando,
                        nombre = estado.nombre,
                        telefono = estado.telefono,
                        errorEdicion = estado.errorEdicion,
                        mensaje = estado.mensaje,
                        versionFoto = versionFoto,
                        angosto = angosto,
                        onFoto = pedirFoto,
                        onEditar = viewModel::iniciarEdicion,
                        onNombre = { viewModel.editar(nombre = it) },
                        onTelefono = { viewModel.editar(telefono = it) },
                        onGuardar = viewModel::guardar,
                        onCancelar = viewModel::cancelarEdicion,
                        modifier = mod
                    )
                }
                val ajustes: @Composable (Modifier) -> Unit = { mod ->
                    Column(mod, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TarjetaAjustes(
                            angosto = angosto,
                            habilitado = !estado.guardando,
                            onSeguridad = { onCambiarContrasena(perfil.correo.orEmpty()) },
                            onAcercaDe = { verAcercaDe = true }
                        )
                        BotonCerrarSesion(enabled = !estado.guardando) { confirmarSalida = true }
                    }
                }
                if (horizontal) {
                    Row(contenidoMod, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        tarjetaPerfil(Modifier.weight(1f))
                        ajustes(Modifier.weight(1f))
                    }
                } else {
                    Column(contenidoMod, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        tarjetaPerfil(Modifier)
                        ajustes(Modifier)
                    }
                }
            }
        }
    }
}

/** Encabezado azul con flecha, título y el auto de la portada tenue a la derecha. */
@Composable
private fun EncabezadoPerfil(
    horizontal: Boolean,
    puedeActualizar: Boolean,
    puedeVolver: Boolean,
    onVolver: () -> Unit,
    onActualizar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .fondoAuth()
    ) {
        Image(
            painter = painterResource(R.drawable.img_welcome_auto),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(if (horizontal) 100.dp else 120.dp)
                .offset(x = 36.dp, y = if (horizontal) 0.dp else (-10).dp)
                .alpha(0.30f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = 6.dp, end = 8.dp,
                top = if (horizontal) 10.dp else 20.dp,
                bottom = if (horizontal) 60.dp else 80.dp
            )
        ) {
            IconButton(onClick = onVolver, enabled = puedeVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    "Mi perfil",
                    color = Color.White,
                    fontSize = if (horizontal) 26.sp else 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Gestiona tu cuenta y preferencias",
                    color = Color(0xFFDCE6F8),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onActualizar, enabled = puedeActualizar) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Actualizar",
                    tint = Color.White.copy(alpha = if (puedeActualizar) 1f else 0.4f)
                )
            }
        }
    }
}

@Composable
private fun TarjetaBase(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    val c = coloresCliente()
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = c.superficie,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, c.borde),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(content = contenido)
    }
}

@Composable
private fun TarjetaPerfil(
    perfil: PerfilResponse,
    editando: Boolean,
    guardando: Boolean,
    nombre: String,
    telefono: String,
    errorEdicion: String?,
    mensaje: String?,
    versionFoto: Int,
    angosto: Boolean,
    onFoto: () -> Unit,
    onEditar: () -> Unit,
    onNombre: (String) -> Unit,
    onTelefono: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier
) {
    val c = coloresCliente()
    TarjetaBase(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 16.dp)
        ) {
            AvatarPerfil(
                perfil = perfil,
                version = versionFoto,
                tamano = if (angosto) 80.dp else 96.dp,
                onCambiar = onFoto
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    perfil.nombre?.takeIf { it.isNotBlank() } ?: "Sin nombre",
                    fontSize = if (angosto) 19.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.texto,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                perfil.rol?.takeIf { it.isNotBlank() }?.let { rol ->
                    Surface(shape = RoundedCornerShape(14.dp), color = c.azulSuave) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = c.azul, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(rol.uppercase(), color = c.azul, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
            if (!editando) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    onClick = onEditar,
                    shape = RoundedCornerShape(20.dp),
                    color = c.azulSuave,
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = if (angosto) 10.dp else 14.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar perfil", tint = c.azulOscuro, modifier = Modifier.size(18.dp))
                        if (!angosto) {
                            Spacer(Modifier.width(6.dp))
                            Text("Editar", color = c.azulOscuro, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = c.borde, modifier = Modifier.padding(horizontal = 18.dp))

        if (editando) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = onNombre,
                    label = { Text("Nombre") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    singleLine = true,
                    enabled = !guardando,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = onTelefono,
                    label = { Text("Teléfono") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    supportingText = { Text("Opcional. Déjalo vacío para quitar el teléfono.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !guardando,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                errorEdicion?.let { Text(it, color = c.rojo, fontSize = 14.sp) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCancelar,
                        enabled = !guardando,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("Cancelar", color = c.azulOscuro) }
                    Button(
                        onClick = onGuardar,
                        enabled = !guardando,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MarcaFuerte, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text(if (guardando) "Guardando..." else "Guardar", fontWeight = FontWeight.SemiBold) }
                }
            }
        } else {
            FilaDato(Icons.Outlined.Email, "Correo", perfil.correo?.takeIf { it.isNotBlank() } ?: "No disponible")
            HorizontalDivider(color = c.borde, modifier = Modifier.padding(start = 66.dp, end = 18.dp))
            val tel = perfil.telefono?.trim()?.takeIf { it.isNotEmpty() }
            FilaDato(Icons.Outlined.Phone, "Teléfono", tel ?: "No registrado") {
                if (tel == null) {
                    OutlinedButton(
                        onClick = onEditar,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, c.azul),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(38.dp)
                    ) { Text("Agregar", color = c.azul, fontWeight = FontWeight.SemiBold) }
                }
            }
            HorizontalDivider(color = c.borde, modifier = Modifier.padding(start = 66.dp, end = 18.dp))
            FilaDato(Icons.Outlined.Person, "Rol", perfil.rol?.uppercase() ?: "No disponible", destacado = true)
            mensaje?.let {
                Text(
                    it,
                    color = Exito,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp)
                )
            }
        }
    }
}

/** Foto de perfil (si eligió una) o la inicial del correo, con botón de cámara. */
@Composable
private fun AvatarPerfil(perfil: PerfilResponse, version: Int, tamano: Dp, onCambiar: () -> Unit) {
    val c = coloresCliente()
    val contexto = LocalContext.current
    val foto by produceState<ImageBitmap?>(initialValue = null, perfil.idUsuario, version) {
        value = withContext(Dispatchers.IO) { FotoVehiculoLocal.cargarPerfil(contexto, perfil.idUsuario)?.asImageBitmap() }
    }
    val base = perfil.correo?.trim()?.takeIf { it.isNotEmpty() } ?: perfil.nombre?.trim()
    val letra = base?.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
    val paleta = listOf(
        0xFF1A73E8, 0xFFD93025, 0xFF188038, 0xFFE37400,
        0xFF9334E6, 0xFF00897B, 0xFFC2185B, 0xFF3949AB
    )
    val color = Color(paleta[(base?.lowercase()?.hashCode() ?: 0).mod(paleta.size)])
    Box(Modifier.size(tamano)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(if (letra != null) color else c.azulSuave)
                .border(3.dp, c.azulSuave, CircleShape)
                .clickable(onClick = onCambiar),
            contentAlignment = Alignment.Center
        ) {
            val actual = foto
            when {
                actual != null -> Image(
                    actual,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                letra != null -> Text(letra, color = Color.White, fontSize = (tamano.value * 0.42f).sp, fontWeight = FontWeight.Bold)
                else -> Icon(Icons.Outlined.Person, contentDescription = null, tint = c.azul, modifier = Modifier.size(tamano / 2))
            }
        }
        Surface(
            onClick = onCambiar,
            shape = CircleShape,
            color = MarcaFuerte,
            border = BorderStroke(2.dp, c.superficie),
            modifier = Modifier.align(Alignment.BottomEnd).size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Cambiar foto de perfil", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun FilaDato(
    icono: ImageVector,
    etiqueta: String,
    valor: String,
    destacado: Boolean = false,
    accion: @Composable () -> Unit = {}
) {
    val c = coloresCliente()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Icon(icono, contentDescription = null, tint = c.texto, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f)) {
            Text(etiqueta, color = c.textoSecundario, fontSize = 13.sp)
            Text(
                valor,
                color = c.texto,
                fontSize = 16.sp,
                fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        accion()
    }
}

@Composable
private fun TarjetaAjustes(
    angosto: Boolean,
    habilitado: Boolean,
    onSeguridad: () -> Unit,
    onAcercaDe: () -> Unit
) {
    val c = coloresCliente()
    val contexto = LocalContext.current
    val oscuro = Apariencia.modoOscuro ?: isSystemInDarkTheme()
    TarjetaBase {
        // Apariencia: claro / oscuro
        val selector: @Composable (Modifier) -> Unit = { mod ->
            SelectorModo(oscuro = oscuro, modifier = mod) { Apariencia.cambiar(contexto, it) }
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = c.texto, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(22.dp))
                Column(Modifier.weight(1f)) {
                    Text("Apariencia", color = c.texto, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Cambia el modo de la aplicación", color = c.textoSecundario, fontSize = 13.sp)
                }
                if (!angosto) {
                    Spacer(Modifier.width(8.dp))
                    selector(Modifier.width(190.dp))
                }
            }
            if (angosto) {
                Spacer(Modifier.height(12.dp))
                selector(Modifier.fillMaxWidth().padding(start = 48.dp))
            }
        }
        HorizontalDivider(color = c.borde, modifier = Modifier.padding(horizontal = 18.dp))
        FilaOpcion(Icons.Outlined.VerifiedUser, "Seguridad", "Cambia tu contraseña", habilitado, onSeguridad)
        HorizontalDivider(color = c.borde, modifier = Modifier.padding(horizontal = 18.dp))
        FilaOpcion(Icons.Outlined.Info, "Acerca de", "Versión ${BuildConfig.VERSION_NAME}", true, onAcercaDe)
    }
}

/** Control segmentado Claro | Oscuro. */
@Composable
private fun SelectorModo(oscuro: Boolean, modifier: Modifier, onCambiar: (Boolean) -> Unit) {
    val c = coloresCliente()
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(c.azulSuave)
            .padding(3.dp)
    ) {
        listOf(false to "Claro", true to "Oscuro").forEach { (valor, texto) ->
            val activo = oscuro == valor
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (activo) MarcaFuerte else Color.Transparent)
                    .clickable(enabled = !activo) { onCambiar(valor) }
            ) {
                val color = if (activo) Color.White else c.texto
                Icon(
                    if (valor) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(texto, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FilaOpcion(icono: ImageVector, titulo: String, detalle: String, habilitado: Boolean, onClick: () -> Unit) {
    val c = coloresCliente()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Icon(icono, contentDescription = null, tint = c.texto, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, color = c.texto, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(detalle, color = c.textoSecundario, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.texto)
    }
}

@Composable
private fun BotonCerrarSesion(enabled: Boolean, onClick: () -> Unit) {
    val c = coloresCliente()
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = c.rojoSuave,
        border = BorderStroke(1.5.dp, c.rojo.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = c.rojo)
            Spacer(Modifier.width(10.dp))
            Text("Cerrar sesión", color = c.rojo, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
