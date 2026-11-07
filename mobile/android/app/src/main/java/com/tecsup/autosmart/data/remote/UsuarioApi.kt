package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.PerfilResponse
import retrofit2.Response
import retrofit2.http.GET

interface UsuarioApi {
    @GET("users/me")
    suspend fun obtenerPerfil(): Response<PerfilResponse>
}
