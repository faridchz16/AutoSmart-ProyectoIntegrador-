package com.tecsup.autosmart.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Apuntamos directamente a la IP física de tu interfaz Wi-Fi
    private const val BASE_URL = "http://192.168.1.63:8081/"

    // Aplicamos variable global para guardar el token JWT al iniciar sesión
    var token: String? = null

    // Cliente OkHttp que intercepta las peticiones y adjunta el Token de autorización
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            // Adjunta el encabezado Authorization si el token existe
            token?.let { jwtToken ->
                requestBuilder.header("Authorization", "Bearer $jwtToken")
            }

            chain.proceed(requestBuilder.build())
        }
        .build()

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    val vehiculoApi: VehiculoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Asignamos el OkHttpClient con el interceptor
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VehiculoApi::class.java)
    }
}