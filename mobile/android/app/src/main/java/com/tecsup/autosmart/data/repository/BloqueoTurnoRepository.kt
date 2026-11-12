package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.remote.ReservaApi
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.mensajeDeError
import com.tecsup.autosmart.model.BloqueoRequest
import com.tecsup.autosmart.model.BloqueoResponse
import kotlinx.coroutines.CancellationException

// T33.2: bloqueo temporal del turno mientras el cliente confirma la reserva
class BloqueoTurnoRepository(
    private val api: ReservaApi = RetrofitClient.reservaApi
) {
    suspend fun bloquear(datos: BloqueoRequest): Result<BloqueoResponse> = try {
        val respuesta = api.bloquearTurno(datos)
        val bloqueo = respuesta.body()
        when {
            !respuesta.isSuccessful -> Result.failure(
                Exception(mensajeDeError(respuesta.errorBody()?.string(), respuesta.code()))
            )
            bloqueo == null || !bloqueoValido(bloqueo, datos) -> Result.failure(
                Exception("El servidor no confirmó el bloqueo del turno.")
            )
            else -> Result.success(bloqueo)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo reservar el turno. Revisa tu conexión", e))
    }

    suspend fun liberar(idBloqueo: Long): Result<Unit> = try {
        val respuesta = api.liberarTurno(idBloqueo)
        // 404: el bloqueo ya venció o ya se usó al confirmar la cita; no es un error para el usuario
        if (respuesta.isSuccessful || respuesta.code() == 404) Result.success(Unit)
        else Result.failure(Exception(mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

internal fun bloqueoValido(bloqueo: BloqueoResponse, datos: BloqueoRequest): Boolean =
    bloqueo.idBloqueo > 0 &&
        bloqueo.segundosRestantes > 0 &&
        (bloqueo.fecha == null || bloqueo.fecha == datos.fecha) &&
        (bloqueo.horaInicio == null || bloqueo.horaInicio.take(5) == datos.horaInicio.take(5))
