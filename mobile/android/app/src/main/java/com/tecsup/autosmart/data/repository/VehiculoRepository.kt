package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.VehiculoRequest
import com.tecsup.autosmart.data.model.VehiculoResponse
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.data.network.VehiculoApi
import com.tecsup.autosmart.data.network.mensajeDeError
import kotlinx.coroutines.CancellationException

class VehiculoRepository(private val api: VehiculoApi = RetrofitClient.vehiculoApi) {
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
