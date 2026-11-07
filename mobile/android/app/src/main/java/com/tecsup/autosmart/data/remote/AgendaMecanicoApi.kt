package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.AgendaMecanicoDto
import com.tecsup.autosmart.model.CitaDetalleMecanicoDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AgendaMecanicoApi {
    @GET("mechanics/me/appointments")
    suspend fun listar(@Query("desde") desde: String,
                       @Query("hasta") hasta: String): Response<List<AgendaMecanicoDto>>

    @GET("appointments/{id}")
    suspend fun detalle(@Path("id") idCita: Long): Response<CitaDetalleMecanicoDto>
}
