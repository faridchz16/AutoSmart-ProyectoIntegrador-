
package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.model.CitaRequest
import com.tecsup.autosmart.model.CitaCreadaResponse
import com.tecsup.autosmart.data.remote.CitaApi
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.mensajeDeError
import com.tecsup.autosmart.model.SeguimientoDto
import com.tecsup.autosmart.model.ReprogramarRequest
import com.tecsup.autosmart.model.coincideHorario
import kotlinx.coroutines.CancellationException

class CitaRepository(
    private val api: CitaApi = RetrofitClient.citaApi
) {
    suspend fun seguimiento(idCita: Long): Result<SeguimientoDto> = try {
        require(idCita > 0) { "La cita no es válida." }
        val respuesta = api.seguimiento(idCita)
        if (!respuesta.isSuccessful) {
            Result.failure(Exception(mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())))
        } else {
            val datos = respuesta.body()
            require(datos != null && seguimientoValido(datos, idCita)) {
                "El servidor no devolvió un seguimiento válido para esta cita."
            }
            Result.success(datos)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }


    suspend fun crearCita(
        request: CitaRequest
    ): Result<CitaCreadaResponse> {
        return try {
            val response = api.crearCita(request)

            if (response.isSuccessful) {
                val cita = response.body()

                if (cita != null) {
                    Result.success(cita)
                } else {
                    Result.failure(
                        Exception("El servidor no devolvió los datos de la cita")
                    )
                }
            } else {
                Result.failure(
                    Exception(
                        mensajeDeError(
                            response.errorBody()?.string(),
                            response.code()
                        )
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    e.message ?: "No se pudo conectar con el servidor",
                    e
                )
            )
        }
    }

    suspend fun misCitas(): Result<List<CitaCreadaResponse>> = try {
        val response = api.listarMisCitas(futuras = false)
        val citas = response.body()
        when {
            !response.isSuccessful -> Result.failure(
                Exception(mensajeDeError(response.errorBody()?.string(), response.code()))
            )
            citas == null -> Result.failure(
                Exception("El servidor no devolvió la lista de citas")
            )
            else -> Result.success(citas)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión", e))
    }

    suspend fun reprogramarCita(idCita: Long, datos: ReprogramarRequest): Result<CitaCreadaResponse> = try {
        val respuesta = api.reprogramarCita(idCita, datos)
        val cita = respuesta.body()
        when {
            !respuesta.isSuccessful -> {
                val mensaje = mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())
                Result.failure(if (respuesta.code() == 408 || respuesta.code() >= 500)
                    ReprogramacionInciertaException(mensaje) else Exception(mensaje))
            }
            cita == null || cita.idCita != idCita || cita.estado != "PENDIENTE" || !coincideHorario(cita, datos) ->
                Result.failure(ReprogramacionInciertaException("El servidor no confirmó el nuevo horario."))
            else -> Result.success(cita)
        }
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { Result.failure(ReprogramacionInciertaException("No se pudo confirmar la reprogramación por un error de conexión.", e)) }

    suspend fun cancelarCita(idCita: Long): Result<CitaCreadaResponse> = try {
        val response = api.cancelarCita(idCita)
        val cita = response.body()
        when {
            !response.isSuccessful -> {
                val mensaje = mensajeDeError(response.errorBody()?.string(), response.code())
                Result.failure(
                    if (response.code() == 408 || response.code() >= 500) {
                        CancelacionInciertaException(mensaje)
                    } else {
                        Exception(mensaje)
                    }
                )
            }
            cita == null || cita.idCita != idCita || cita.estado != "CANCELADA" ->
                Result.failure(CancelacionInciertaException("El servidor no confirmó la cancelación"))
            else -> Result.success(cita)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(
            CancelacionInciertaException("No se pudo confirmar la cancelación por un error de conexión", e)
        )
    }
}

class CancelacionInciertaException(mensaje: String, causa: Throwable? = null) :
    Exception(mensaje, causa)

internal fun seguimientoValido(datos: SeguimientoDto, idCita: Long): Boolean {
    val hitos = datos.lineaDeTiempo ?: return false
    return idCita > 0 && datos.idCita == idCita && !datos.estado.isNullOrBlank() &&
        datos.porcentajeAvance?.let { it in 0..100 } == true &&
        hitos.isNotEmpty() && hitos.all {
            it != null && !it.clave.isNullOrBlank() && !it.titulo.isNullOrBlank() && it.completado != null
        } && hitos.distinctBy { it?.clave }.size == hitos.size
}

class ReprogramacionInciertaException(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)
