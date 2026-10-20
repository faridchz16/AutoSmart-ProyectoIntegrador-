package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.MecanicoDto
import com.tecsup.autosmart.data.model.TurnoDto
import retrofit2.Response
import retrofit2.http.GET
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
}