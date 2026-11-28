package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.CalificacionDto
import com.tecsup.autosmart.model.CalificacionRequest
import com.tecsup.autosmart.model.CostoOrdenDto
import com.tecsup.autosmart.model.MetodoPagoDto
import com.tecsup.autosmart.model.PagoDto
import com.tecsup.autosmart.model.PagoRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

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

    // T64.2: comprobante en PDF del pago aprobado
    @Streaming
    @GET("payments/{id}/receipt")
    suspend fun comprobante(@Path("id") idPago: Long): Response<ResponseBody>

    // T65.2: calificar al mecánico (1 a 5 estrellas y comentario opcional)
    @POST("work-orders/{id}/rating")
    suspend fun calificar(@Path("id") idOrden: Long, @Body request: CalificacionRequest): Response<CalificacionDto>
}
