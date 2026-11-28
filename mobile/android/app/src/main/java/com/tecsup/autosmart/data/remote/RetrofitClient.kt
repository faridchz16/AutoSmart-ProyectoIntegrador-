package com.tecsup.autosmart.data.remote

import android.content.Context
import com.tecsup.autosmart.BuildConfig
import com.tecsup.autosmart.data.local.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // TEC2.3: la URL ya no está fija; viene de BuildConfig (local.properties -> autosmart.baseUrl)
    private val BASE_URL: String = BuildConfig.BASE_URL


    var token: String? = null

    /**
     * Se activa cuando el servidor responde 401 a una petición que sí llevaba token:
     * la sesión venció (el JWT dura 60 min). MainActivity lo escucha y pide iniciar sesión otra vez.
     */
    val sesionExpirada = kotlinx.coroutines.flow.MutableStateFlow(false)

    private var tokenManager: TokenManager? = null

    fun init(context: Context) {
        tokenManager = TokenManager(context)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            var conToken = original.header("Authorization") != null
            if (!conToken) {
                val activeToken = token ?: runBlocking {
                    tokenManager?.getToken?.firstOrNull()
                }
                if (!activeToken.isNullOrEmpty()) {
                    requestBuilder.header("Authorization", "Bearer $activeToken")
                    conToken = true
                }
            }

            val respuesta = chain.proceed(requestBuilder.build())
            if (respuesta.code == 401 && conToken) sesionExpirada.value = true
            respuesta
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

    // Sprint 4 · T61.2, T62.2 y T63.2: costo final, métodos de pago y pago en línea
    val pagoApi: PagoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PagoApi::class.java)
    }

    // Perfil profesional del mecánico (GET/PUT /mechanics/me)
    val mecanicoApi: MecanicoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MecanicoApi::class.java)
    }

    // Sprint 4 · T75.2: chatbot del taller (POST /chatbot/messages)
    val chatbotApi: ChatbotApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatbotApi::class.java)
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