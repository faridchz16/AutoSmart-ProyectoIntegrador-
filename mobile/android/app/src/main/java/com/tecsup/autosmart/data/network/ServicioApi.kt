package com.tecsup.autosmart.data.network

import com.tecsup.autosmart.data.model.Servicio
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ServicioApi {
    @GET("services")
    suspend fun listar(@Query("q") q: String?): Response<List<Servicio>>

    @GET("services/{id}")
    suspend fun detalle(@Path("id") id: Long): Response<Servicio>
}