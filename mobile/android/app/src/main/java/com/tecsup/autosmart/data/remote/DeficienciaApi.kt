package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.DeficienciaRequest
import com.tecsup.autosmart.model.DeficienciaResponse
import retrofit2.Response
import retrofit2.http.*

interface DeficienciaApi {
    @GET("work-orders/{id}/deficiencies")
    suspend fun listar(@Path("id") idOrden: Long): Response<List<DeficienciaResponse>>

    @POST("work-orders/{id}/deficiencies")
    suspend fun registrar(@Path("id") idOrden: Long, @Body request: DeficienciaRequest): Response<DeficienciaResponse>
}
