package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.AgendaMecanicoDto
import com.tecsup.autosmart.data.model.CitaDetalleMecanicoDto
import com.tecsup.autosmart.data.network.AgendaMecanicoApi
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.data.network.mensajeDeError
import kotlinx.coroutines.CancellationException

class AgendaMecanicoRepository(private val api: AgendaMecanicoApi = RetrofitClient.agendaMecanicoApi) {
    suspend fun listar(fecha: String): Result<List<AgendaMecanicoDto>> = try {
        val respuesta = api.listar(fecha, fecha)
        val citas = respuesta.body()
        when {
            !respuesta.isSuccessful -> Result.failure(Exception(
                mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())))
            citas == null || citas.any { it.idCita <= 0 } || citas.distinctBy { it.idCita }.size != citas.size ->
                Result.failure(Exception("El servidor no devolvió una agenda válida."))
            else -> Result.success(citas)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo consultar la agenda. Revisa tu conexión.", e))
    }

    suspend fun detalle(idCita: Long): Result<CitaDetalleMecanicoDto> = try {
        val respuesta = api.detalle(idCita)
        val cita = respuesta.body()
        when {
            !respuesta.isSuccessful -> Result.failure(Exception(
                mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())))
            cita == null || idCita <= 0 || cita.idCita != idCita ->
                Result.failure(Exception("El servidor no devolvió el detalle de esta cita."))
            else -> Result.success(cita)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo consultar la cita. Revisa tu conexión.", e))
    }
}
