package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.OrdenTrabajoDto
import retrofit2.Response
import retrofit2.http.GET

interface OrdenTrabajoApi {
    @GET("mechanics/me/work-orders")
    suspend fun listar(): Response<List<OrdenTrabajoDto>>
}
