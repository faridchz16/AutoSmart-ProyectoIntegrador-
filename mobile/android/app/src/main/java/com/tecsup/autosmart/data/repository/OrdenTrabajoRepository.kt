package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.OrdenTrabajoDto
import com.tecsup.autosmart.data.network.OrdenTrabajoApi
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.data.network.mensajeDeError
import kotlinx.coroutines.CancellationException

class OrdenTrabajoRepository(private val api: OrdenTrabajoApi = RetrofitClient.ordenTrabajoApi) {
    suspend fun listar(): Result<List<OrdenTrabajoDto>> = try {
        val respuesta = api.listar()
        val ordenes = respuesta.body()
        when {
            !respuesta.isSuccessful -> Result.failure(Exception(
                mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())))
            ordenes == null || ordenes.any { it.idOrden <= 0 || it.idCita <= 0 ||
                it.estado !in setOf("EN_PROCESO", "ESPERA_APROBACION") } ||
                ordenes.distinctBy { it.idOrden }.size != ordenes.size ->
                Result.failure(Exception("El servidor no devolvió órdenes activas válidas."))
            else -> Result.success(ordenes)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudieron consultar las órdenes. Revisa tu conexión.", e))
    }
}
