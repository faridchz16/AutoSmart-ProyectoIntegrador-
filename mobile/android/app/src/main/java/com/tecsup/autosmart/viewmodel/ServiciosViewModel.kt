package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.model.Servicio
import com.tecsup.autosmart.data.repository.ServicioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ListaServiciosUiState(
    val cargando: Boolean = true,
    val busqueda: String = "",
    val servicios: List<Servicio> = emptyList(),
    val error: String? = null
)

data class DetalleServicioUiState(
    val cargando: Boolean = false,
    val servicio: Servicio? = null,
    val error: String? = null
)

class ServiciosViewModel : ViewModel() {

    private val repo = ServicioRepository()

    private val _lista = MutableStateFlow(ListaServiciosUiState())
    val lista: StateFlow<ListaServiciosUiState> = _lista

    private val _detalle = MutableStateFlow(DetalleServicioUiState())
    val detalle: StateFlow<DetalleServicioUiState> = _detalle

    private var jobBusqueda: Job? = null

    init { reintentar() }

    /** Se llama en cada letra. Espera un instante antes de consultar para no saturar el servidor. */
    fun buscar(texto: String) {
        _lista.value = _lista.value.copy(busqueda = texto)
        jobBusqueda?.cancel()
        jobBusqueda = viewModelScope.launch {
            delay(350)
            consultar(texto.trim())
        }
    }

    fun reintentar() {
        jobBusqueda?.cancel()
        jobBusqueda = viewModelScope.launch { consultar(_lista.value.busqueda.trim()) }
    }

    private suspend fun consultar(q: String) {
        _lista.value = _lista.value.copy(cargando = true, error = null)
        repo.listar(q)
            .onSuccess { _lista.value = _lista.value.copy(cargando = false, servicios = it) }
            .onFailure {
                _lista.value = _lista.value.copy(
                    cargando = false,
                    error = it.message ?: "No se pudo cargar el catálogo"
                )
            }
    }

    fun cargarDetalle(id: Long) {
        // Muestra de inmediato lo que ya vino en la lista y luego lo refresca desde el servidor
        val enLista = _lista.value.servicios.firstOrNull { it.idServicio == id }
        _detalle.value = DetalleServicioUiState(cargando = enLista == null, servicio = enLista)
        viewModelScope.launch {
            repo.detalle(id)
                .onSuccess { _detalle.value = DetalleServicioUiState(servicio = it) }
                .onFailure {
                    _detalle.value = _detalle.value.copy(
                        cargando = false,
                        error = if (_detalle.value.servicio == null)
                            it.message ?: "No se pudo cargar el servicio" else null
                    )
                }
        }
    }
}
