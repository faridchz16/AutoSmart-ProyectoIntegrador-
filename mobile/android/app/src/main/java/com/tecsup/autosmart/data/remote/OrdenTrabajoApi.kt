package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.DetalleMantenimientoDto
import com.tecsup.autosmart.model.OrdenTrabajoDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OrdenTrabajoApi {
    @GET("mechanics/me/work-orders")
    suspend fun listar(): Response<List<OrdenTrabajoDto>>

    @GET("work-orders/{id}/detail")
    suspend fun detalle(@Path("id") idOrden: Long): Response<DetalleMantenimientoDto>
}
