package com.tecsup.autosmart.ui.mecanico

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.autosmart.R
import com.tecsup.autosmart.data.repository.MAX_ANIOS_EXPERIENCIA
import com.tecsup.autosmart.data.repository.MAX_ESPECIALIDADES
import com.tecsup.autosmart.ui.auth.fondoAuth
import com.tecsup.autosmart.ui.cliente.coloresCliente
import com.tecsup.autosmart.viewmodel.MecanicoUiState
import com.tecsup.autosmart.viewmodel.MecanicoViewModel
import com.tecsup.autosmart.viewmodel.SUGERENCIAS_ESPECIALIDAD

private val MarcaFuerte = Color(0xFF1150E8)
private val Verde = Color(0xFF16A34A)
private val Ambar = Color(0xFFB45309)

private fun textoAnios(n: Int) = if (n == 1) "1 año de experiencia" else "$n años de experiencia"

/** Perfil profesional del mecánico: años de experiencia y especialidades (GET/PUT /mechanics/me). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerfilMecanicoScreen(viewModel: MecanicoViewModel, onBack: () -> Unit, onLogout: () -> Unit = {}) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(viewModel) { viewModel.cargarSiNecesario() }
    var confirmarSalir by remember { mutableStateOf(false) }
    var confirmarCerrarSesion by remember { mutableStateOf(false) }
    val volver: () -> Unit = {
        if (estado.hayCambios) { confirmarSalir = true } else { onBack() }
        Unit
    }
    BackHandler(enabled = estado.guardando || estado.hayCambios) { if (!estado.guardando) confirmarSalir = true }

    val c = coloresCliente()
    val horizontal = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (confirmarSalir) {
        AlertDialog(
            onDismissRequest = { confirmarSalir = false },
            title = { Text("Cambios sin guardar") },
            text = { Text("Tienes cambios en tu perfil que todavía no guardaste.") },
            confirmButton = {
                TextButton(onClick = { confirmarSalir = false; viewModel.guardar() }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalir = false; viewModel.descartar(); onBack() }) { Text("Descartar") }
            }
        )
    }

    if (confirmarCerrarSesion) {
        AlertDialog(
            onDismissRequest = { confirmarCerrarSesion = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres salir de tu cuenta?") },
            confirmButton = {
                TextButton(onClick = { confirmarCerrarSesion = false; onLogout() }) {
                    Text("Cerrar sesión", color = c.rojo)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarCerrarSesion = false }) { Text("Cancelar") } }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.fondo)
            .verticalScroll(rememberScrollState())
    ) {
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
                    .align(Alignment.CenterEnd)
                    .height(if (horizontal) 96.dp else 112.dp)
                    .offset(x = 36.dp)
                    .alpha(0.28f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp, end = 16.dp, top = if (horizontal) 10.dp else 20.dp,
                    bottom = if (horizontal) 56.dp else 72.dp)
            ) {
                IconButton(onClick = volver, enabled = !estado.guardando) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Column(Modifier.padding(start = 4.dp)) {
                    Text("Mi perfil profesional", color = Color.White, fontSize = if (horizontal) 24.sp else 26.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Así te verán los clientes al reservar", color = Color(0xFFDCE6F8), fontSize = 14.sp)
                }
            }
        }

        val mod = Modifier.padding(horizontal = 16.dp).offset(y = if (horizontal) (-40).dp else (-52).dp)
        when {
            estado.perfil == null && estado.cargando -> Tarjeta(mod) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.azul)
                }
            }
            estado.perfil == null -> Tarjeta(mod) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(estado.error ?: "No se pudo cargar tu perfil.", color = c.rojo, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::cargar,
                        colors = ButtonDefaults.buttonColors(containerColor = MarcaFuerte, contentColor = Color.White)) {
                        Text("Reintentar")
                    }
                }
            }
            horizontal -> Row(mod, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VistaPrevia(estado, Modifier.weight(1f))
                Edicion(estado, viewModel, Modifier.weight(1.2f))
            }
            else -> Column(mod, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VistaPrevia(estado, Modifier)
                Edicion(estado, viewModel, Modifier)
            }
        }
        // Cerrar sesión (igual que en el perfil del cliente)
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .offset(y = if (horizontal) (-24).dp else (-36).dp)
                .padding(bottom = 8.dp)
        ) {
            BotonCerrarSesionMecanico(enabled = !estado.guardando) { confirmarCerrarSesion = true }
        }
    }
}

@Composable
private fun BotonCerrarSesionMecanico(enabled: Boolean, onClick: () -> Unit) {
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

/** Tarjeta como la ve el cliente al elegir mecánico (refleja los cambios aún sin guardar). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VistaPrevia(estado: MecanicoUiState, modifier: Modifier) {
    val c = coloresCliente()
    val perfil = estado.perfil ?: return
    Tarjeta(modifier) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clip(CircleShape).background(MarcaFuerte).padding(3.dp)) {
                AvatarMecanico(perfil.nombre ?: perfil.correo, 80.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(perfil.nombre ?: "Mecánico", color = c.texto, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
            perfil.correo?.let { Text(it, color = c.textoSecundario, fontSize = 13.sp) }
            Spacer(Modifier.height(6.dp))
            Text(textoAnios(estado.anios), color = c.azul, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            if (estado.especialidades.isEmpty()) {
                Text("Sin especialidades", color = c.textoSecundario, fontSize = 13.sp)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    estado.especialidades.forEach { e ->
                        Surface(shape = RoundedCornerShape(12.dp), color = c.azulSuave) {
                            Text(e, color = c.azulOscuro, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }
            if (estado.hayCambios) {
                Spacer(Modifier.height(10.dp))
                Text("Vista previa con cambios sin guardar", color = Ambar, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Edicion(estado: MecanicoUiState, viewModel: MecanicoViewModel, modifier: Modifier) {
    val c = coloresCliente()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (estado.perfilIncompleto) {
            Surface(shape = RoundedCornerShape(16.dp), color = Ambar.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Ambar.copy(alpha = 0.4f))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Ambar)
                    Spacer(Modifier.width(10.dp))
                    Text("Mientras no guardes al menos una especialidad, los clientes no podrán elegirte al reservar.",
                        color = c.texto, fontSize = 13.sp)
                }
            }
        }

        // Años de experiencia
        Tarjeta {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Años de experiencia", color = c.texto, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("De 0 a $MAX_ANIOS_EXPERIENCIA", color = c.textoSecundario, fontSize = 12.sp)
                }
                FilledTonalIconButton(onClick = { viewModel.cambiarAnios(-1) }, enabled = !estado.guardando && estado.anios > 0) {
                    Icon(Icons.Filled.Remove, contentDescription = "Restar un año")
                }
                Text(estado.anios.toString(), color = c.texto, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
                FilledTonalIconButton(onClick = { viewModel.cambiarAnios(1) },
                    enabled = !estado.guardando && estado.anios < MAX_ANIOS_EXPERIENCIA) {
                    Icon(Icons.Filled.Add, contentDescription = "Sumar un año")
                }
            }
        }

        // Especialidades
        Tarjeta {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Especialidades", color = c.texto, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${estado.especialidades.size}/$MAX_ESPECIALIDADES", color = c.textoSecundario, fontSize = 12.sp)
                }
                if (estado.especialidades.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        estado.especialidades.forEach { e ->
                            InputChip(
                                selected = true,
                                onClick = { viewModel.quitar(e) },
                                enabled = !estado.guardando,
                                label = { Text(e) },
                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Quitar $e", modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
                val lleno = estado.especialidades.size >= MAX_ESPECIALIDADES
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = estado.texto,
                        onValueChange = viewModel::editarTexto,
                        label = { Text("Nueva especialidad") },
                        singleLine = true,
                        enabled = !estado.guardando && !lleno,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.agregar() }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { viewModel.agregar() },
                        enabled = !estado.guardando && !lleno && estado.texto.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MarcaFuerte, contentColor = Color.White)
                    ) { Icon(Icons.Filled.Add, contentDescription = "Agregar especialidad") }
                }
                val sugerencias = SUGERENCIAS_ESPECIALIDAD.filter { s -> estado.especialidades.none { it.equals(s, ignoreCase = true) } }
                if (sugerencias.isNotEmpty() && !lleno) {
                    Text("Sugerencias", color = c.textoSecundario, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sugerencias.forEach { s ->
                            SuggestionChip(onClick = { viewModel.agregar(s) }, enabled = !estado.guardando, label = { Text(s) })
                        }
                    }
                }
            }
        }

        estado.errorEdicion?.let { Text(it, color = c.rojo, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp)) }
        estado.mensaje?.let { Text(it, color = Verde, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp)) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = viewModel::descartar,
                enabled = estado.hayCambios && !estado.guardando,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text("Descartar") }
            Button(
                onClick = viewModel::guardar,
                enabled = estado.hayCambios && !estado.guardando,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MarcaFuerte, contentColor = Color.White),
                modifier = Modifier.weight(1.4f).height(50.dp)
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (estado.guardando) "Guardando..." else "Guardar cambios", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
