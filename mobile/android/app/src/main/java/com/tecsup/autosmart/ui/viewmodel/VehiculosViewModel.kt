package com.tecsup.autosmart.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.model.VehiculoResponse
import com.tecsup.autosmart.data.repository.EliminacionInciertaException
import com.tecsup.autosmart.data.repository.VehiculoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VehiculosEstado(
    val vehiculos: List<VehiculoResponse> = emptyList(),
    val cargando: Boolean = true,
    val errorCarga: String? = null,
    val seleccionado: VehiculoResponse? = null,
    val ocupado: Boolean = false,
    val pendienteVerificacion: Long? = null,
    val errorOperacion: String? = null,
    val mensaje: String? = null
)

class VehiculosViewModel(
    private val listar: suspend () -> Result<List<VehiculoResponse>> = { VehiculoRepository().listar() },
    private val eliminar: suspend (Long) -> Result<Unit> = { VehiculoRepository().eliminar(it) }
) : ViewModel() {
    private val _estado = MutableStateFlow(VehiculosEstado())
    val estado: StateFlow<VehiculosEstado> = _estado
    private var iniciado = false
    private var carga: Job? = null
    private var operacion: Job? = null

    fun cargarSiNecesario() { if (!iniciado) cargar() }

    fun cargar() {
        if (carga?.isActive == true || _estado.value.ocupado ||
            _estado.value.pendienteVerificacion != null) return
        iniciado = true
        _estado.value = _estado.value.copy(cargando = true, errorCarga = null)
        carga = viewModelScope.launch {
            val resultado = consultar()
            currentCoroutineContext().ensureActive()
            resultado.onSuccess {
                _estado.value = _estado.value.copy(cargando = false, vehiculos = it)
            }.onFailure {
                _estado.value = _estado.value.copy(cargando = false,
                    errorCarga = it.message ?: "No se pudieron cargar tus vehículos.")
            }
        }
    }

    fun seleccionar(vehiculo: VehiculoResponse) {
        val actual = _estado.value
        if (actual.ocupado || actual.cargando || actual.pendienteVerificacion != null) return
        val seleccionado = actual.vehiculos.firstOrNull { it.idVehiculo == vehiculo.idVehiculo } ?: return
        _estado.value = actual.copy(seleccionado = seleccionado, errorOperacion = null, mensaje = null)
    }

    fun cerrarConfirmacion() {
        if (!_estado.value.ocupado) _estado.value = _estado.value.copy(seleccionado = null)
    }

    fun confirmarEliminacion() {
        val actual = _estado.value
        val vehiculo = actual.seleccionado ?: return
        if (actual.ocupado || actual.pendienteVerificacion != null) return
        _estado.value = actual.copy(ocupado = true, errorOperacion = null, mensaje = null)
        operacion = viewModelScope.launch {
            val resultado = try {
                eliminar(vehiculo.idVehiculo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(EliminacionInciertaException(e))
            }
            currentCoroutineContext().ensureActive()
            resultado.onSuccess {
                _estado.value = _estado.value.copy(
                    vehiculos = _estado.value.vehiculos.filterNot { it.idVehiculo == vehiculo.idVehiculo },
                    seleccionado = null, ocupado = false, mensaje = "Vehículo eliminado correctamente.")
            }.onFailure {
                if (it is EliminacionInciertaException) {
                    _estado.value = _estado.value.copy(seleccionado = null,
                        pendienteVerificacion = vehiculo.idVehiculo)
                    verificar(vehiculo.idVehiculo)
                } else {
                    _estado.value = _estado.value.copy(seleccionado = null, ocupado = false,
                        errorOperacion = it.message ?: "No se pudo eliminar el vehículo.")
                }
            }
        }
    }

    fun comprobarEstado() {
        val id = _estado.value.pendienteVerificacion ?: return
        if (_estado.value.ocupado) return
        _estado.value = _estado.value.copy(ocupado = true)
        operacion = viewModelScope.launch { verificar(id) }
    }

    private suspend fun consultar(): Result<List<VehiculoResponse>> = try {
        listar()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo consultar tus vehículos. Revisa tu conexión.", e))
    }

    private suspend fun verificar(id: Long) {
        val resultado = consultar()
        currentCoroutineContext().ensureActive()
        resultado.onSuccess { vehiculos ->
            val ausente = vehiculos.none { it.idVehiculo == id }
            _estado.value = _estado.value.copy(ocupado = false, pendienteVerificacion = null,
                vehiculos = vehiculos,
                mensaje = if (ausente) "Se verificó que el vehículo ya no está en tu lista." else null,
                errorOperacion = if (ausente) null else
                    "No se confirmó la eliminación. El vehículo sigue registrado; puedes volver a intentarlo.")
        }.onFailure {
            _estado.value = _estado.value.copy(ocupado = false,
                errorOperacion = "No se pudo comprobar si el vehículo fue eliminado. Revisa tu conexión y pulsa Comprobar estado antes de reintentar.")
        }
    }

    fun limpiar() {
        carga?.cancel()
        operacion?.cancel()
        carga = null
        operacion = null
        iniciado = false
        _estado.value = VehiculosEstado()
    }
}
