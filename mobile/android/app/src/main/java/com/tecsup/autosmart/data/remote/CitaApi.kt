
package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.CitaRequest
import com.tecsup.autosmart.model.CitaCreadaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.PUT
import retrofit2.http.Path

interface CitaApi {

    @POST("appointments")
    suspend fun crearCita(
        @Body request: CitaRequest
    ): Response<CitaCreadaResponse>

    @GET("appointments/mine")
    suspend fun listarMisCitas(
        @Query("futuras") futuras: Boolean = false
    ): Response<List<CitaCreadaResponse>>

    @PUT("appointments/{id}/cancel")
    suspend fun cancelarCita(
        @Path("id") idCita: Long
    ): Response<CitaCreadaResponse>
}
