package com.tecsup.autosmart.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.model.*
import com.tecsup.autosmart.data.repository.DeficienciaRepository
import com.tecsup.autosmart.data.repository.RegistroDeficienciaInciertoException
import com.tecsup.autosmart.data.repository.coincide
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DeficienciasEstado(
    val idOrden: Long = 0,
    val editable: Boolean = false,
    val cargando: Boolean = true,
    val cargado: Boolean = false,
    val guardando: Boolean = false,
    val lista: List<DeficienciaResponse> = emptyList(),
    val categoria: String = "",
    val severidad: String = "",
    val descripcion: String = "",
    val verificar: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
) {
    val modificado get() = categoria.isNotBlank() || severidad.isNotBlank() || descripcion.isNotEmpty()
}

class DeficienciasViewModel(
    private val consultar: suspend (Long) -> Result<List<DeficienciaResponse>> = { DeficienciaRepository().listar(it) },
    private val enviar: suspend (Long, DeficienciaRequest) -> Result<DeficienciaResponse> = { id, request ->
        DeficienciaRepository().registrar(id, request)
    }
) : ViewModel() {
    private val _estado = MutableStateFlow(DeficienciasEstado())
    val estado: StateFlow<DeficienciasEstado> = _estado
    private var tarea: Job? = null
    private var idsPrevios = emptySet<Long>()

    fun iniciar(idOrden: Long, editable: Boolean) {
        if (_estado.value.idOrden == idOrden) return
        limpiar()
        _estado.value = DeficienciasEstado(idOrden = idOrden, editable = editable)
        cargar()
    }

    fun cargar() {
        val actual = _estado.value
        if (actual.idOrden <= 0 || tarea?.isActive == true) return
        _estado.value = actual.copy(cargando = true, error = null, mensaje = null)
        tarea = viewModelScope.launch {
            val result = try { consultar(actual.idOrden) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            currentCoroutineContext().ensureActive()
            result.onSuccess { lista ->
                if (actual.verificar) {
                    val request = DeficienciaRequest(actual.categoria, actual.severidad, actual.descripcion.trim())
                    val nuevas = lista.filter { it.idDeficiencia !in idsPrevios && coincide(it, request) }
                    if (nuevas.size == 1) {
                        _estado.value = DeficienciasEstado(actual.idOrden, actual.editable, cargando = false,
                            cargado = true, lista = lista, mensaje = "Registro confirmado en el servidor.")
                    } else {
                        _estado.value = actual.copy(cargando = false, cargado = true, lista = lista,
                            verificar = nuevas.size > 1, error = if (nuevas.isEmpty())
                                "No aparece el registro en la lista consultada. Revisa las deficiencias antes de volver a enviarlo."
                            else "Hay varios registros coincidentes. Revisa la lista antes de continuar.")
                    }
                } else _estado.value = actual.copy(cargando = false, cargado = true, lista = lista, error = null)
            }.onFailure {
                _estado.value = actual.copy(cargando = false, error = it.message ?: "No se pudo cargar la lista.")
            }
        }
    }

    private fun puedeEditar() = _estado.value.let { it.editable && it.cargado && !it.cargando && !it.guardando && !it.verificar }

    fun editar(categoria: String? = null, severidad: String? = null, descripcion: String? = null) {
        if (!puedeEditar() || (categoria != null && categoria !in categoriasDeficiencia) ||
            (severidad != null && severidad !in severidadesDeficiencia) || (descripcion?.length ?: 0) > 500) return
        val actual = _estado.value
        _estado.value = actual.copy(categoria = categoria ?: actual.categoria, severidad = severidad ?: actual.severidad,
            descripcion = descripcion ?: actual.descripcion, error = null, mensaje = null)
    }

    fun guardar() {
        if (!puedeEditar() || tarea?.isActive == true) return
        val actual = _estado.value
        val request = DeficienciaRequest(actual.categoria, actual.severidad, actual.descripcion.trim())
        validarDeficiencia(request)?.let { _estado.value = actual.copy(error = it); return }
        idsPrevios = actual.lista.map { it.idDeficiencia }.toSet()
        _estado.value = actual.copy(guardando = true, error = null, mensaje = null)
        tarea = viewModelScope.launch {
            val result = try { enviar(actual.idOrden, request) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(RegistroDeficienciaInciertoException("Comprueba la lista antes de reenviar.", e)) }
            currentCoroutineContext().ensureActive()
            result.onSuccess { body ->
                _estado.value = DeficienciasEstado(actual.idOrden, actual.editable, cargando = false, cargado = true,
                    lista = actual.lista.filterNot { it.idDeficiencia == body.idDeficiencia } + body,
                    mensaje = "Deficiencia registrada por el servidor.")
            }.onFailure {
                _estado.value = actual.copy(guardando = false, verificar = it is RegistroDeficienciaInciertoException,
                    error = it.message ?: "No se pudo registrar. Tus datos se conservan.")
            }
        }
    }

    fun limpiar() {
        tarea?.cancel()
        tarea = null
        idsPrevios = emptySet()
        _estado.value = DeficienciasEstado()
    }
}
