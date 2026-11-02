package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.PerfilResponse
import retrofit2.Response
import retrofit2.http.GET

interface UsuarioApi {
    @GET("users/me")
    suspend fun obtenerPerfil(): Response<PerfilResponse>
}
