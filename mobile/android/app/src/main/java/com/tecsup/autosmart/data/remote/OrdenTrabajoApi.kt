package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.OrdenTrabajoDto
import retrofit2.Response
import retrofit2.http.GET

interface OrdenTrabajoApi {
    @GET("mechanics/me/work-orders")
    suspend fun listar(): Response<List<OrdenTrabajoDto>>
}
