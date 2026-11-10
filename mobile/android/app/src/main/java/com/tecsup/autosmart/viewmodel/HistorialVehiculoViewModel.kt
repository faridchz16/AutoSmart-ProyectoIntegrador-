package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.repository.VehiculoRepository
import com.tecsup.autosmart.model.MantenimientoHistorialDto
import com.tecsup.autosmart.model.VehiculoResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistorialVehiculoEstado(
    val vehiculos: List<VehiculoResponse> = emptyList(),
    val seleccionado: Long? = null,
    val mantenimientos: List<MantenimientoHistorialDto> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class HistorialVehiculoViewModel(
    private val repository: VehiculoRepository = VehiculoRepository()
) : ViewModel() {
    private val _estado = MutableStateFlow(HistorialVehiculoEstado())
    val estado = _estado.asStateFlow()
    private var consulta: Job? = null

    fun iniciar(idVehiculo: Long) {
        consulta?.cancel()
        _estado.value = HistorialVehiculoEstado()
        consulta = viewModelScope.launch {
            repository.listar().fold(
                onSuccess = { vehiculos ->
                    val id = vehiculos.firstOrNull { it.idVehiculo == idVehiculo }?.idVehiculo
                        ?: vehiculos.firstOrNull()?.idVehiculo
                    _estado.value = HistorialVehiculoEstado(
                        vehiculos = vehiculos,
                        seleccionado = id,
                        cargando = id != null
                    )
                    if (id != null) cargarHistorial(id)
                },
                onFailure = { error ->
                    _estado.update { it.copy(cargando = false, error = error.message) }
                }
            )
        }
    }

    fun seleccionar(idVehiculo: Long) {
        consulta?.cancel()
        _estado.update {
            it.copy(seleccionado = idVehiculo, mantenimientos = emptyList(), cargando = true, error = null)
        }
        consulta = viewModelScope.launch { cargarHistorial(idVehiculo) }
    }

    private suspend fun cargarHistorial(idVehiculo: Long) {
        repository.historial(idVehiculo).fold(
            onSuccess = { historial ->
                _estado.update { it.copy(cargando = false, mantenimientos = historial.mantenimientos.orEmpty()) }
            },
            onFailure = { error ->
                _estado.update { it.copy(cargando = false, error = error.message) }
            }
        )
    }

    fun limpiar() {
        consulta?.cancel()
        _estado.value = HistorialVehiculoEstado()
    }
}
