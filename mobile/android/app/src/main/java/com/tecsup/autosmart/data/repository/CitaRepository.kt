
package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.CitaRequest
import com.tecsup.autosmart.data.model.CitaCreadaResponse
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.data.network.mensajeDeError

class CitaRepository {

    suspend fun crearCita(
        request: CitaRequest
    ): Result<CitaCreadaResponse> {
        return try {
            val response = RetrofitClient.citaApi.crearCita(request)

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
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    e.message ?: "No se pudo conectar con el servidor",
                    e
                )
            )
        }
    }
}
