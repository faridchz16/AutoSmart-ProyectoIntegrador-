package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.repository.CitaRepository
import com.tecsup.autosmart.model.SeguimientoDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SeguimientoEstado(
    val idCita: Long? = null,
    val cargando: Boolean = true,
    val seguimiento: SeguimientoDto? = null,
    val error: String? = null
)

class SeguimientoViewModel(
    private val consultar: suspend (Long) -> Result<SeguimientoDto> = { CitaRepository().seguimiento(it) }
) : ViewModel() {
    private val _estado = MutableStateFlow(SeguimientoEstado())
    val estado = _estado.asStateFlow()
    private var consulta: Job? = null

    fun iniciar(idCita: Long) {
        if (_estado.value.idCita != idCita) cargar(idCita)
    }

    fun actualizar() {
        if (consulta?.isActive == true) return
        _estado.value.idCita?.let(::cargar)
    }

    private fun cargar(idCita: Long) {
        consulta?.cancel()
        _estado.value = SeguimientoEstado(idCita)
        consulta = viewModelScope.launch {
            val resultado = try { consultar(idCita) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            currentCoroutineContext().ensureActive()
            _estado.value = resultado.fold(
                onSuccess = { SeguimientoEstado(idCita, cargando = false, seguimiento = it) },
                onFailure = { SeguimientoEstado(idCita, cargando = false,
                    error = it.message ?: "No se pudo consultar el seguimiento.") }
            )
        }
    }

    fun limpiar() {
        consulta?.cancel()
        consulta = null
        _estado.value = SeguimientoEstado()
    }
}
