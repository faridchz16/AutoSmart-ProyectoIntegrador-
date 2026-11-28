package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.PerfilMecanicoDto
import com.tecsup.autosmart.model.PerfilMecanicoRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface MecanicoApi {
    @GET("mechanics/me")
    suspend fun perfil(): Response<PerfilMecanicoDto>

    @PUT("mechanics/me")
    suspend fun guardarPerfil(@Body request: PerfilMecanicoRequest): Response<PerfilMecanicoDto>
}
