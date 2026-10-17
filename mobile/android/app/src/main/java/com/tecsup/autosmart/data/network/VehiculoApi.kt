package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.VehiculoRequest
import com.tecsup.autosmart.data.model.VehiculoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface VehiculoApi {

    @GET("vehicles")
    suspend fun listarVehiculos(): Response<List<VehiculoResponse>>

    @POST("vehicles")
    suspend fun registrarVehiculo(
        @Body request: VehiculoRequest
    ): Response<VehiculoResponse>
}