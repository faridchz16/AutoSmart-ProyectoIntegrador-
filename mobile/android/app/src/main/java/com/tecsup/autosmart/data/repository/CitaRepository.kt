
package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.CitaRequest
import com.tecsup.autosmart.data.model.CitaCreadaResponse
import com.tecsup.autosmart.data.network.CitaApi
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.data.network.mensajeDeError
import kotlinx.coroutines.CancellationException

class CitaRepository(
    private val api: CitaApi = RetrofitClient.citaApi
) {

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
