package com.tecsup.autosmart.viewmodel

import com.tecsup.autosmart.model.PerfilRequest
import com.tecsup.autosmart.model.validarPerfil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.model.PerfilResponse
import com.tecsup.autosmart.data.repository.PerfilRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PerfilUiState(
    val cargando: Boolean = true,
    val perfil: PerfilResponse? = null,
    val editando: Boolean = false,
    val nombre: String = "",
    val telefono: String = "",
    val guardando: Boolean = false,
    val errorEdicion: String? = null,
    val mensaje: String? = null,
    val error: String? = null
)

class PerfilViewModel(
    private val consultar: suspend () -> Result<PerfilResponse> = {
        PerfilRepository().obtenerPerfil()
    },
    private val actualizar: suspend (Int, PerfilRequest) -> Result<PerfilResponse> = { id, datos ->
        PerfilRepository().actualizarPerfil(id, datos)
    }
) : ViewModel() {
    private val _estado = MutableStateFlow(PerfilUiState())
    val estado: StateFlow<PerfilUiState> = _estado
    private var consulta: Job? = null
    private var iniciada = false
    private var guardado: Job? = null

    fun cargarSiNecesario() {
        if (!iniciada) cargarPerfil()
    }

    fun cargarPerfil() {
        if (consulta?.isActive == true || _estado.value.editando || _estado.value.guardando) return
        iniciada = true
        _estado.value = PerfilUiState(cargando = true)
        consulta = viewModelScope.launch {
            try {
                val resultado = consultar()
                currentCoroutineContext().ensureActive()
                resultado.onSuccess {
                    _estado.value = PerfilUiState(cargando = false, perfil = it)
                }.onFailure {
                    _estado.value = PerfilUiState(
                        cargando = false,
                        error = it.message ?: "No se pudo cargar tu perfil"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.value = PerfilUiState(
                    cargando = false,
                    error = e.message ?: "No se pudo cargar tu perfil"
                )
            }
        }
    }

    fun iniciarEdicion() {
        val actual = _estado.value
        val perfil = actual.perfil ?: return
        if (actual.cargando || actual.guardando) return
        _estado.value = actual.copy(editando = true, nombre = perfil.nombre.orEmpty(),
            telefono = perfil.telefono.orEmpty(), errorEdicion = null, mensaje = null)
    }

    fun editar(nombre: String = _estado.value.nombre, telefono: String = _estado.value.telefono) {
        if (!_estado.value.editando || _estado.value.guardando) return
        _estado.value = _estado.value.copy(nombre = nombre, telefono = telefono, errorEdicion = null)
    }

    fun cancelarEdicion() {
        if (!_estado.value.guardando) _estado.value = _estado.value.copy(
            editando = false, nombre = "", telefono = "", errorEdicion = null)
    }

    fun guardar() {
        val actual = _estado.value
        val perfil = actual.perfil ?: return
        if (!actual.editando || actual.guardando) return
        val error = validarPerfil(actual.nombre, actual.telefono)
        if (error != null) { _estado.value = actual.copy(errorEdicion = error); return }
        _estado.value = actual.copy(guardando = true, errorEdicion = null)
        guardado = viewModelScope.launch {
            val resultado = try {
                actualizar(perfil.idUsuario, PerfilRequest(actual.nombre.trim(), actual.telefono.trim()))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(Exception("No se pudo confirmar la actualización. Revisa tu conexión.", e)) }
            currentCoroutineContext().ensureActive()
            resultado.onSuccess { confirmado ->
                _estado.value = PerfilUiState(cargando = false, perfil = confirmado,
                    mensaje = "Perfil actualizado correctamente.")
            }.onFailure {
                _estado.value = _estado.value.copy(guardando = false,
                    errorEdicion = it.message ?: "No se pudo actualizar tu perfil.")
            }
        }
    }

    fun limpiar() {
        guardado?.cancel()
        guardado = null
        consulta?.cancel()
        consulta = null
        iniciada = false
        _estado.value = PerfilUiState()
    }
}
