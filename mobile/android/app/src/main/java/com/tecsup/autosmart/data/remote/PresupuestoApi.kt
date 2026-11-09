package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.DecisionPresupuestoRequest
import com.tecsup.autosmart.model.PresupuestoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface PresupuestoApi {
    @GET("appointments/{idCita}/quote")
    suspend fun obtener(@Path("idCita") idCita: Long): Response<PresupuestoResponse>

    @PUT("quotes/{idPresupuesto}/decision")
    suspend fun decidir(
        @Path("idPresupuesto") idPresupuesto: Long,
        @Body request: DecisionPresupuestoRequest
    ): Response<PresupuestoResponse>
}
