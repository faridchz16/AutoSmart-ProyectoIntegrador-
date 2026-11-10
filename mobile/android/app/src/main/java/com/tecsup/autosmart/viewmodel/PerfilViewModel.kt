package com.tecsup.autosmart.viewmodel

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
    val error: String? = null
)

class PerfilViewModel(
    private val consultar: suspend () -> Result<PerfilResponse> = {
        PerfilRepository().obtenerPerfil()
    }
) : ViewModel() {
    private val _estado = MutableStateFlow(PerfilUiState())
    val estado: StateFlow<PerfilUiState> = _estado
    private var consulta: Job? = null
    private var iniciada = false

    fun cargarSiNecesario() {
        if (!iniciada) cargarPerfil()
    }

    fun cargarPerfil() {
        if (consulta?.isActive == true) return
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

    fun limpiar() {
        consulta?.cancel()
        consulta = null
        iniciada = false
        _estado.value = PerfilUiState()
    }
}
