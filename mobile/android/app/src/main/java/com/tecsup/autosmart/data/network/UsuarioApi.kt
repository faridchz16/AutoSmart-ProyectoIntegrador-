package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.PerfilResponse
import retrofit2.Response
import com.tecsup.autosmart.data.model.PerfilRequest
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.GET

interface UsuarioApi {
    @PUT("users/me")
    suspend fun actualizarPerfil(@Body datos: PerfilRequest): Response<PerfilResponse>

    @GET("users/me")
    suspend fun obtenerPerfil(): Response<PerfilResponse>
}
