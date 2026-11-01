package com.tecsup.autosmart.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.model.CitaCreadaResponse
import com.tecsup.autosmart.data.repository.CitaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MisCitasUiState(
    val cargando: Boolean = true,
    val citas: List<CitaCreadaResponse> = emptyList(),
    val error: String? = null
)

class MisCitasViewModel(
    private val consultar: suspend () -> Result<List<CitaCreadaResponse>> = {
        CitaRepository().misCitas()
    }
) : ViewModel() {
    private val _estado = MutableStateFlow(MisCitasUiState())
    val estado: StateFlow<MisCitasUiState> = _estado
    private var consulta: Job? = null
    private var iniciada = false

    fun cargarSiNecesario() {
        if (!iniciada) cargarCitas()
    }

    fun cargarCitas() {
        if (consulta?.isActive == true) return
        iniciada = true
        _estado.value = MisCitasUiState(cargando = true)
        consulta = viewModelScope.launch {
            try {
                val resultado = consultar()
                currentCoroutineContext().ensureActive()
                resultado.onSuccess {
                    _estado.value = MisCitasUiState(cargando = false, citas = it)
                }.onFailure {
                    _estado.value = MisCitasUiState(
                        cargando = false,
                        error = it.message ?: "No se pudieron cargar tus citas"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.value = MisCitasUiState(
                    cargando = false,
                    error = e.message ?: "No se pudieron cargar tus citas"
                )
            }
        }
    }

    fun limpiar() {
        consulta?.cancel()
        consulta = null
        iniciada = false
        _estado.value = MisCitasUiState()
    }
}
