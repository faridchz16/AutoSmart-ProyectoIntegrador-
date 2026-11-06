package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.PresupuestoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PresupuestoApi {
    @GET("appointments/{idCita}/quote")
    suspend fun obtener(@Path("idCita") idCita: Long): Response<PresupuestoResponse>
}
