package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.HistorialVehiculoDto
import com.tecsup.autosmart.model.VehiculoRequest
import com.tecsup.autosmart.model.VehiculoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.POST

interface VehiculoApi {

    @GET("vehicles/{id}/history")
    suspend fun historial(@Path("id") id: Long): Response<HistorialVehiculoDto>

    @DELETE("vehicles/{id}")
    suspend fun eliminarVehiculo(@Path("id") id: Long): Response<Unit>

    @PUT("vehicles/{id}")
    suspend fun actualizarVehiculo(
        @Path("id") id: Long,
        @Body request: VehiculoRequest
    ): Response<VehiculoResponse>

    @GET("vehicles")
    suspend fun listarVehiculos(): Response<List<VehiculoResponse>>

    @POST("vehicles")
    suspend fun registrarVehiculo(
        @Body request: VehiculoRequest
    ): Response<VehiculoResponse>
}
