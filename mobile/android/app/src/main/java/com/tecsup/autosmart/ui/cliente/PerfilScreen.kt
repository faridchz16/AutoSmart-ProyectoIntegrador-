package com.tecsup.autosmart.ui.cliente

import androidx.activity.compose.BackHandler
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.autosmart.viewmodel.PerfilViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onNavigateBack: () -> Unit = {},
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil") },
                navigationIcon = {
                    IconButton(enabled = !estado.guardando, onClick = {
                        if (estado.editando) viewModel.cancelarEdicion() else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(
                        enabled = !estado.cargando && !estado.editando && !estado.guardando,
                        onClick = { viewModel.cargarPerfil() }
                    ) { Text("Actualizar") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                estado.cargando -> {
                    CircularProgressIndicator()
                    Text("Cargando tu perfil...")
                }
                estado.error != null -> {
                    Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.cargarPerfil() }) { Text("Reintentar") }
                }
                else -> estado.perfil?.let { perfil ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (estado.editando) {
                                OutlinedTextField(value = estado.nombre,
                                    onValueChange = { viewModel.editar(nombre = it) },
                                    label = { Text("Nombre") }, singleLine = true,
                                    enabled = !estado.guardando, modifier = Modifier.fillMaxWidth())
                            } else CampoPerfil("Nombre", perfil.nombre)
                            HorizontalDivider()
                            CampoPerfil("Correo", perfil.correo)
                            HorizontalDivider()
                            if (estado.editando) {
                                OutlinedTextField(value = estado.telefono,
                                    onValueChange = { viewModel.editar(telefono = it) },
                                    label = { Text("Teléfono") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    enabled = !estado.guardando, modifier = Modifier.fillMaxWidth())
                                Text("Opcional. Déjalo vacío para quitar el teléfono.")
                            } else CampoPerfil("Teléfono", perfil.telefono, "No registrado")
                            HorizontalDivider()
                            CampoPerfil("Rol", perfil.rol)
                            estado.errorEdicion?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            estado.mensaje?.let { Text(it) }
                            if (estado.editando) {
                                Button(onClick = viewModel::guardar, enabled = !estado.guardando) {
                                    Text(if (estado.guardando) "Guardando..." else "Guardar cambios")
                                }
                                TextButton(onClick = viewModel::cancelarEdicion, enabled = !estado.guardando) {
                                    Text("Cancelar edición")
                                }
                            } else Button(onClick = viewModel::iniciarEdicion) { Text("Editar perfil") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoPerfil(etiqueta: String, valor: String?, ausente: String = "No disponible") {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor?.trim()?.takeIf { it.isNotEmpty() } ?: ausente,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
