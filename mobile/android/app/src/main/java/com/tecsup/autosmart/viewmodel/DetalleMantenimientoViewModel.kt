package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.repository.FotoRepository
import com.tecsup.autosmart.data.repository.OrdenTrabajoRepository
import com.tecsup.autosmart.model.DetalleMantenimientoDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetalleMantenimientoEstado(
    val idOrden: Long? = null,
    val cargando: Boolean = true,
    val detalle: DetalleMantenimientoDto? = null,
    val error: String? = null
)

class DetalleMantenimientoViewModel(
    private val consultar: suspend (Long) -> Result<DetalleMantenimientoDto> = {
        OrdenTrabajoRepository().detalle(it)
    },
    private val verFoto: suspend (Long) -> Result<ByteArray> = { FotoRepository().obtener(it) }
) : ViewModel() {
    private val _estado = MutableStateFlow(DetalleMantenimientoEstado())
    val estado = _estado.asStateFlow()
    private var consulta: Job? = null

    fun iniciar(idOrden: Long) {
        if (_estado.value.idOrden != idOrden) cargar(idOrden)
    }

    fun actualizar() {
        if (consulta?.isActive != true) _estado.value.idOrden?.let(::cargar)
    }

    private fun cargar(idOrden: Long) {
        consulta?.cancel()
        _estado.value = DetalleMantenimientoEstado(idOrden)
        consulta = viewModelScope.launch {
            val respuesta = try {
                consultar(idOrden)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            currentCoroutineContext().ensureActive()
            _estado.value = respuesta.fold(
                onSuccess = { DetalleMantenimientoEstado(idOrden, cargando = false, detalle = it) },
                onFailure = { DetalleMantenimientoEstado(idOrden, cargando = false,
                    error = it.message ?: "No se pudo consultar el mantenimiento.") }
            )
        }
    }

    suspend fun obtenerFoto(idFoto: Long): Result<ByteArray> {
        val permitida = _estado.value.detalle?.deficiencias.orEmpty().any { deficiencia ->
            deficiencia?.fotos.orEmpty().any { it.idFoto == idFoto && it.url == "/photos/$idFoto" }
        }
        return if (idFoto > 0 && permitida) verFoto(idFoto)
        else Result.failure(IllegalArgumentException("La foto no pertenece a esta orden."))
    }

    fun limpiar() {
        consulta?.cancel()
        consulta = null
        _estado.value = DetalleMantenimientoEstado()
    }
}
