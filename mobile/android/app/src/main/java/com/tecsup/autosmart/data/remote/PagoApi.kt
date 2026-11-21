package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.CostoOrdenDto
import com.tecsup.autosmart.model.MetodoPagoDto
import com.tecsup.autosmart.model.PagoDto
import com.tecsup.autosmart.model.PagoRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PagoApi {
    // T61.2: resumen del costo final de la orden
    @GET("work-orders/{id}/cost")
    suspend fun costo(@Path("id") idOrden: Long): Response<CostoOrdenDto>

    // T62.2: métodos de pago habilitados
    @GET("payment-methods")
    suspend fun metodos(): Response<List<MetodoPagoDto>>

    // T63.2: pago en línea (sandbox)
    @POST("payments")
    suspend fun pagar(@Body request: PagoRequest): Response<PagoDto>
}
