package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.model.VehiculoRequest
import com.tecsup.autosmart.model.VehiculoResponse
import com.tecsup.autosmart.data.repository.VehiculoRepository
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EditarVehiculoEstado(
    val original: VehiculoResponse? = null,
    val marca: String = "",
    val modelo: String = "",
    val anio: String = "",
    val kilometraje: String = "",
    val guardando: Boolean = false,
    val actualizado: VehiculoResponse? = null,
    val error: String? = null
) {
    fun validar(anioActual: Int = Calendar.getInstance().get(Calendar.YEAR)): String? = when {
        original == null -> "Vuelve a seleccionar el vehículo."
        marca.isBlank() -> "Ingresa la marca."
        modelo.isBlank() -> "Ingresa el modelo."
        anio.toIntOrNull() == null || anio.toInt() !in 1900..(anioActual + 1) -> "Ingresa un año válido."
        kilometraje.toIntOrNull() == null || kilometraje.toInt() < 0 -> "Ingresa un kilometraje válido."
        kilometraje.toInt() < original.kilometraje -> "El kilometraje no puede ser menor al registrado (${original.kilometraje} km)."
        else -> null
    }
}

class EditarVehiculoViewModel(
    private val actualizar: suspend (Long, VehiculoRequest) -> Result<VehiculoResponse> = { id, datos ->
        VehiculoRepository().actualizar(id, datos)
    }
) : ViewModel() {
    private val _estado = MutableStateFlow(EditarVehiculoEstado())
    val estado: StateFlow<EditarVehiculoEstado> = _estado
    private var solicitud: Job? = null

    fun iniciar(vehiculo: VehiculoResponse) {
        if (_estado.value.guardando) return
        _estado.value = EditarVehiculoEstado(
            original = vehiculo, marca = vehiculo.marca, modelo = vehiculo.modelo,
            anio = vehiculo.anio.toString(), kilometraje = vehiculo.kilometraje.toString()
        )
    }

    fun editar(marca: String = _estado.value.marca, modelo: String = _estado.value.modelo,
               anio: String = _estado.value.anio, kilometraje: String = _estado.value.kilometraje) {
        if (_estado.value.guardando || _estado.value.actualizado != null) return
        _estado.value = _estado.value.copy(marca = marca, modelo = modelo, anio = anio,
            kilometraje = kilometraje, error = null)
    }

    fun guardar() {
        val datos = _estado.value
        if (datos.guardando || datos.actualizado != null) return
        val error = datos.validar()
        if (error != null) {
            _estado.value = datos.copy(error = error)
            return
        }
        val original = datos.original ?: return
        _estado.value = datos.copy(guardando = true, error = null)
        solicitud = viewModelScope.launch {
            try {
                val resultado = actualizar(original.idVehiculo, VehiculoRequest(
                    placa = original.placa, marca = datos.marca.trim(), modelo = datos.modelo.trim(),
                    anio = datos.anio.toInt(), kilometraje = datos.kilometraje.toInt()
                ))
                currentCoroutineContext().ensureActive()
                resultado.onSuccess {
                    _estado.value = _estado.value.copy(guardando = false, actualizado = it)
                }.onFailure {
                    _estado.value = _estado.value.copy(guardando = false,
                        error = it.message ?: "No se pudo actualizar el vehículo.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(guardando = false,
                    error = "No se pudo confirmar la actualización. Revisa tu conexión.")
            }
        }
    }

    fun limpiar() {
        solicitud?.cancel()
        solicitud = null
        _estado.value = EditarVehiculoEstado()
    }
}
