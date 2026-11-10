package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.model.HistorialVehiculoDto
import com.tecsup.autosmart.model.VehiculoRequest
import com.tecsup.autosmart.model.VehiculoResponse
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.VehiculoApi
import com.tecsup.autosmart.data.remote.mensajeDeError
import kotlinx.coroutines.CancellationException

class VehiculoRepository(private val api: VehiculoApi = RetrofitClient.vehiculoApi) {
    suspend fun historial(id: Long): Result<HistorialVehiculoDto> = try {
        val respuesta = api.historial(id)
        val historial = respuesta.body()
        when {
            !respuesta.isSuccessful -> Result.failure(Exception(
                mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())
            ))
            historial == null || historial.idVehiculo != id ->
                Result.failure(Exception("El servidor no devolvió el historial del vehículo seleccionado."))
            else -> Result.success(historial)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo consultar el historial. Revisa tu conexión.", e))
    }

    suspend fun listar(): Result<List<VehiculoResponse>> = try {
        val respuesta = api.listarVehiculos()
        when {
            !respuesta.isSuccessful -> Result.failure(Exception(
                mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())))
            respuesta.body() == null -> Result.failure(Exception("El servidor no devolvió la lista de vehículos."))
            else -> Result.success(respuesta.body().orEmpty())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo consultar tus vehículos. Revisa tu conexión.", e))
    }

    suspend fun eliminar(id: Long): Result<Unit> = try {
        val respuesta = api.eliminarVehiculo(id)
        when {
            respuesta.code() == 204 -> Result.success(Unit)
            respuesta.isSuccessful || respuesta.code() in listOf(404, 408) || respuesta.code() >= 500 ->
                Result.failure(EliminacionInciertaException())
            else -> Result.failure(Exception(
                mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(EliminacionInciertaException(e))
    }

    suspend fun actualizar(id: Long, datos: VehiculoRequest): Result<VehiculoResponse> = try {
        val respuesta = api.actualizarVehiculo(id, datos)
        val vehiculo = respuesta.body()
        when {
            !respuesta.isSuccessful -> Result.failure(Exception(
                mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())
            ))
            vehiculo == null || vehiculo.idVehiculo != id || vehiculo.placa != datos.placa ->
                Result.failure(Exception("No se pudo verificar la actualización. Consulta la lista antes de reintentar."))
            else -> Result.success(vehiculo)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo confirmar la actualización. Revisa tu conexión y consulta la lista antes de reintentar.", e))
    }
}

class EliminacionInciertaException(causa: Throwable? = null) : Exception(
    "No se pudo confirmar la eliminación del vehículo.", causa
)
