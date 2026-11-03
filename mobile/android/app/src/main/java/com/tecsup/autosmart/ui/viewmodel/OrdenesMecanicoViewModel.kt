package com.tecsup.autosmart.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.model.OrdenTrabajoDto
import com.tecsup.autosmart.data.repository.OrdenTrabajoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OrdenesMecanicoEstado(
    val cargando: Boolean = true,
    val ordenes: List<OrdenTrabajoDto> = emptyList(),
    val seleccionada: Long? = null,
    val error: String? = null
)

class OrdenesMecanicoViewModel(
    private val consultar: suspend () -> Result<List<OrdenTrabajoDto>> = {
        OrdenTrabajoRepository().listar()
    }
) : ViewModel() {
    private val _estado = MutableStateFlow(OrdenesMecanicoEstado())
    val estado: StateFlow<OrdenesMecanicoEstado> = _estado
    private var iniciada = false
    private var consulta: Job? = null

    fun cargarSiNecesario() { if (!iniciada) actualizar() }

    fun actualizar() {
        if (consulta?.isActive == true) return
        iniciada = true
        val seleccionada = _estado.value.seleccionada
        _estado.value = OrdenesMecanicoEstado(seleccionada = seleccionada)
        consulta = viewModelScope.launch {
            val resultado = try { consultar() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            currentCoroutineContext().ensureActive()
            resultado.onSuccess { ordenes ->
                _estado.value = OrdenesMecanicoEstado(false, ordenes,
                    seleccionada?.takeIf { id -> ordenes.any { it.idOrden == id } })
            }.onFailure {
                _estado.value = OrdenesMecanicoEstado(cargando = false,
                    error = it.message ?: "No se pudieron cargar las órdenes.")
            }
        }
    }

    fun seleccionar(idOrden: Long) {
        if (_estado.value.ordenes.any { it.idOrden == idOrden })
            _estado.value = _estado.value.copy(seleccionada = idOrden)
    }

    fun volverLista() { _estado.value = _estado.value.copy(seleccionada = null) }

    fun limpiar() {
        consulta?.cancel()
        consulta = null
        iniciada = false
        _estado.value = OrdenesMecanicoEstado()
    }
}
