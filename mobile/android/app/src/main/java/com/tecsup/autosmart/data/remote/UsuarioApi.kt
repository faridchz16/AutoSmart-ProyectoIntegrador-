package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.PerfilResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

data class RegistroFcmRequest(val token: String)

interface UsuarioApi {
    @GET("users/me")
    suspend fun obtenerPerfil(): Response<PerfilResponse>

    @PUT("users/me/fcm-token")
    suspend fun registrarFcm(
        @Header("Authorization") authorization: String,
        @Body request: RegistroFcmRequest
    ): Response<Unit>

    @DELETE("users/me/fcm-token")
    suspend fun retirarFcm(@Header("Authorization") authorization: String): Response<Unit>
}
