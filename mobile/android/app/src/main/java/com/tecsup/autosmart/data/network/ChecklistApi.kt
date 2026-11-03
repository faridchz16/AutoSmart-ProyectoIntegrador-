package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.ChecklistRequest
import com.tecsup.autosmart.data.model.ChecklistResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ChecklistApi {
    @GET("work-orders/{id}/checklist")
    suspend fun obtener(@Path("id") idOrden: Long): Response<ChecklistResponse>

    @PUT("work-orders/{id}/checklist")
    suspend fun guardar(@Path("id") idOrden: Long, @Body request: ChecklistRequest): Response<ChecklistResponse>
}
