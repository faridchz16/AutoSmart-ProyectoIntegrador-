package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.VehiculoRequest
import com.tecsup.autosmart.data.model.VehiculoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.POST

interface VehiculoApi {

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
