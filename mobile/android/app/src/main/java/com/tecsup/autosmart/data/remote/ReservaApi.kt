package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.BloqueoRequest
import com.tecsup.autosmart.model.BloqueoResponse
import com.tecsup.autosmart.model.MecanicoDto
import com.tecsup.autosmart.model.TurnoDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReservaApi {
    @GET("services/{id}/mechanics")
    suspend fun mecanicosPorServicio(@Path("id") idServicio: Long): Response<List<MecanicoDto>>

    @GET("services/{id}/slots")
    suspend fun turnos(
        @Path("id") idServicio: Long,
        @Query("fecha") fecha: String,
        @Query("mecanico") idMecanico: Int
    ): Response<List<TurnoDto>>

    // T33.2: bloquear el turno mientras se confirma (10 minutos en el servidor)
    @POST("appointments/holds")
    suspend fun bloquearTurno(@Body datos: BloqueoRequest): Response<BloqueoResponse>

    // T33.2: liberar el turno si el cliente cambia de turno o sale de la reserva
    @DELETE("appointments/holds/{id}")
    suspend fun liberarTurno(@Path("id") idBloqueo: Long): Response<Unit>
}
