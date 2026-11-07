package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.model.*
import com.tecsup.autosmart.data.repository.ChecklistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChecklistEstado(
    val idOrden: Long = 0,
    val editable: Boolean = false,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val cargado: Boolean = false,
    val items: List<ChecklistItemDto> = elementosChecklist.map { ChecklistItemDto(it) },
    val observacionGeneral: String = "",
    val fechaRegistro: String? = null,
    val modificado: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

class ChecklistViewModel(
    private val consultar: suspend (Long) -> Result<ChecklistResponse?> = { ChecklistRepository().obtener(it) },
    private val enviar: suspend (Long, ChecklistRequest) -> Result<ChecklistResponse> = { id, request ->
        ChecklistRepository().guardar(id, request)
    }
) : ViewModel() {
    private val _estado = MutableStateFlow(ChecklistEstado())
    val estado: StateFlow<ChecklistEstado> = _estado
    private var tarea: Job? = null

    fun iniciar(idOrden: Long, editable: Boolean) {
        if (_estado.value.idOrden == idOrden) return
        limpiar()
        _estado.value = ChecklistEstado(idOrden = idOrden, editable = editable)
        cargar()
    }

    fun cargar() {
        val actual = _estado.value
        if (actual.idOrden <= 0 || tarea?.isActive == true || actual.modificado) return
        _estado.value = actual.copy(cargando = true, error = null, mensaje = null)
        tarea = viewModelScope.launch {
            val result = try { consultar(actual.idOrden) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            currentCoroutineContext().ensureActive()
            result.onSuccess { body ->
                _estado.value = actual.copy(cargando = false, cargado = true,
                    items = body?.items ?: elementosChecklist.map { ChecklistItemDto(it) },
                    observacionGeneral = body?.observacionGeneral.orEmpty(), fechaRegistro = body?.fechaRegistro,
                    error = null, mensaje = if (body == null) "Esta orden aún no tiene checklist." else null)
            }.onFailure {
                _estado.value = actual.copy(cargando = false, error = it.message ?: "No se pudo consultar el checklist.")
            }
        }
    }

    private fun puedeEditar() = _estado.value.let { it.editable && it.cargado && !it.cargando && !it.guardando }

    fun cambiarElemento(elemento: String, estado: String? = null, observacion: String? = null) {
        if (!puedeEditar() || (estado != null && estado !in setOf("OK", "OBSERVADO")) ||
            (observacion?.length ?: 0) > 300) return
        _estado.value = _estado.value.copy(items = _estado.value.items.map {
            if (it.elemento == elemento) it.copy(estado = estado ?: it.estado,
                observacion = observacion ?: it.observacion) else it
        }, modificado = true, error = null, mensaje = null)
    }

    fun cambiarGeneral(texto: String) {
        if (!puedeEditar() || texto.length > 300) return
        _estado.value = _estado.value.copy(observacionGeneral = texto, modificado = true, error = null, mensaje = null)
    }

    fun guardar() {
        if (!puedeEditar() || !_estado.value.modificado || tarea?.isActive == true) return
        val actual = _estado.value
        val request = ChecklistRequest(actual.observacionGeneral.trim().ifBlank { null }, actual.items.map {
            it.copy(observacion = it.observacion?.trim()?.ifBlank { null })
        })
        validarChecklist(request)?.let { _estado.value = actual.copy(error = it); return }
        _estado.value = actual.copy(guardando = true, error = null, mensaje = null)
        tarea = viewModelScope.launch {
            val result = try { enviar(actual.idOrden, request) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            currentCoroutineContext().ensureActive()
            result.onSuccess { body ->
                _estado.value = actual.copy(guardando = false, modificado = false,
                    items = body.items.orEmpty(), observacionGeneral = body.observacionGeneral.orEmpty(),
                    fechaRegistro = body.fechaRegistro, error = null, mensaje = "Checklist guardado por el servidor.")
            }.onFailure {
                _estado.value = actual.copy(guardando = false, error = it.message ?: "No se pudo guardar. Tus datos se conservan.")
            }
        }
    }

    fun limpiar() {
        tarea?.cancel()
        tarea = null
        _estado.value = ChecklistEstado()
    }
}
