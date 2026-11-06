package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.FotoCreadaResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface FotoApi {
    @Multipart
    @POST("work-orders/{id}/deficiencies/{idDeficiencia}/photos")
    suspend fun subir(@Path("id") idOrden: Long, @Path("idDeficiencia") idDeficiencia: Long,
                      @Part foto: MultipartBody.Part): Response<FotoCreadaResponse>

    @Streaming
    @GET("photos/{id}")
    suspend fun obtener(@Path("id") idFoto: Long): Response<ResponseBody>
}
