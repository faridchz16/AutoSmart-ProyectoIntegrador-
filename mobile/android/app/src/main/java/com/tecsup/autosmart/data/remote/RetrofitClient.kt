package com.tecsup.autosmart.data.remote

import android.content.Context
import com.tecsup.autosmart.data.local.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.63:8081/"


    var token: String? = null

    private var tokenManager: TokenManager? = null

    fun init(context: Context) {
        tokenManager = TokenManager(context)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            if (original.header("Authorization") == null) {
                val activeToken = token ?: runBlocking {
                    tokenManager?.getToken?.firstOrNull()
                }
                if (!activeToken.isNullOrEmpty()) {
                    requestBuilder.header("Authorization", "Bearer $activeToken")
                }
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
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VehiculoApi::class.java)
    }

    val reservaApi: ReservaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReservaApi::class.java)
    }

    val servicioApi: ServicioApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServicioApi::class.java)
    }


    val citaApi: CitaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CitaApi::class.java)
    }

    val usuarioApi: UsuarioApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UsuarioApi::class.java)
    }

    val presupuestoApi: PresupuestoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PresupuestoApi::class.java)
    }

    val fotoApi: FotoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FotoApi::class.java)
    }

    val deficienciaApi: DeficienciaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeficienciaApi::class.java)
    }

    val checklistApi: ChecklistApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChecklistApi::class.java)
    }

    val ordenTrabajoApi: OrdenTrabajoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OrdenTrabajoApi::class.java)
    }

    val agendaMecanicoApi: AgendaMecanicoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgendaMecanicoApi::class.java)
    }

}