package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.model.AgendaMecanicoDto
import com.tecsup.autosmart.data.repository.AgendaMecanicoRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AgendaMecanicoEstado(
    val fecha: String = LocalDate.now(ZoneId.of("America/Lima")).toString(),
    val cargando: Boolean = true,
    val citas: List<AgendaMecanicoDto> = emptyList(),
    val error: String? = null
)

class AgendaMecanicoViewModel(
    private val consultar: suspend (String) -> Result<List<AgendaMecanicoDto>> = {
        AgendaMecanicoRepository().listar(it)
    }
) : ViewModel() {
    private val _estado = MutableStateFlow(AgendaMecanicoEstado())
    val estado: StateFlow<AgendaMecanicoEstado> = _estado
    private var iniciada = false
    private var consulta: Job? = null

    fun cargarSiNecesario() { if (!iniciada) cargarFecha(_estado.value.fecha) }
    fun actualizar() { cargarFecha(_estado.value.fecha) }

    fun cargarFecha(fecha: String) {
        try { LocalDate.parse(fecha) } catch (e: Exception) { return }
        if (consulta?.isActive == true && _estado.value.fecha == fecha) return
        consulta?.cancel()
        iniciada = true
        _estado.value = AgendaMecanicoEstado(fecha = fecha)
        consulta = viewModelScope.launch {
            val resultado = try { consultar(fecha) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            currentCoroutineContext().ensureActive()
            resultado.onSuccess {
                _estado.value = AgendaMecanicoEstado(fecha, cargando = false, citas = it)
            }.onFailure {
                _estado.value = AgendaMecanicoEstado(fecha, cargando = false,
                    error = it.message ?: "No se pudo cargar la agenda.")
            }
        }
    }

    fun limpiar() {
        consulta?.cancel()
        consulta = null
        iniciada = false
        _estado.value = AgendaMecanicoEstado()
    }
}
