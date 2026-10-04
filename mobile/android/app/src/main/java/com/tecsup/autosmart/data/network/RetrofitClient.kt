package com.tecsup.autosmart.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Se quitó "/api/" para que coincida con @RequestMapping("/auth") de Spring Boot
    private const val BASE_URL = "http://10.0.2.2:8081/"

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}