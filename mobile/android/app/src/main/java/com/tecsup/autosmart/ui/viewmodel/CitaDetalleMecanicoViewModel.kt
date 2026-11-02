package com.tecsup.autosmart.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.model.CitaDetalleMecanicoDto
import com.tecsup.autosmart.data.repository.AgendaMecanicoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CitaDetalleMecanicoEstado(
    val idCita: Long? = null,
    val cargando: Boolean = true,
    val cita: CitaDetalleMecanicoDto? = null,
    val error: String? = null
)

class CitaDetalleMecanicoViewModel(
    private val consultar: suspend (Long) -> Result<CitaDetalleMecanicoDto> = {
        AgendaMecanicoRepository().detalle(it)
    }
) : ViewModel() {
    private val _estado = MutableStateFlow(CitaDetalleMecanicoEstado())
    val estado: StateFlow<CitaDetalleMecanicoEstado> = _estado
    private var consulta: Job? = null

    fun cargar(idCita: Long, forzar: Boolean = false) {
        if (_estado.value.idCita == idCita && (!forzar || consulta?.isActive == true)) return
        consulta?.cancel()
        if (idCita <= 0) {
            _estado.value = CitaDetalleMecanicoEstado(idCita, false, error = "Selecciona una cita válida.")
            return
        }
        _estado.value = CitaDetalleMecanicoEstado(idCita)
        consulta = viewModelScope.launch {
            val resultado = try { consultar(idCita) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            currentCoroutineContext().ensureActive()
            resultado.onSuccess {
                _estado.value = CitaDetalleMecanicoEstado(idCita, cargando = false, cita = it)
            }.onFailure {
                _estado.value = CitaDetalleMecanicoEstado(idCita, cargando = false,
                    error = it.message ?: "No se pudo cargar el detalle.")
            }
        }
    }

    fun limpiar() {
        consulta?.cancel()
        consulta = null
        _estado.value = CitaDetalleMecanicoEstado()
    }
}
