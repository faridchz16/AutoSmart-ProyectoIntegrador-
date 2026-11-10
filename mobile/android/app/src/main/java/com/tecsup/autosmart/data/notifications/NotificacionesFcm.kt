package com.tecsup.autosmart.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.tecsup.autosmart.data.local.TokenManager
import com.tecsup.autosmart.data.remote.RegistroFcmRequest
import com.tecsup.autosmart.data.remote.RetrofitClient
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

internal const val CANAL_VEHICULO_LISTO = "vehiculo_listo"
internal const val TIPO_ORDEN_FINALIZADA = "ORDEN_FINALIZADA"

internal fun citaDeAviso(tipo: String?, idCita: String?): Long? =
    idCita?.toLongOrNull()?.takeIf { tipo == TIPO_ORDEN_FINALIZADA && it > 0 }

internal object NotificacionesFcm {
    private const val TRABAJO_REGISTRO = "autosmart-registro-fcm"
    private val sesion = Mutex()

    fun disponible(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CANAL_VEHICULO_LISTO, "Estado del vehículo",
                    NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun programarRegistro(context: Context) {
        if (!disponible(context)) return
        val trabajo = OneTimeWorkRequestBuilder<RegistroFcmWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            TRABAJO_REGISTRO, ExistingWorkPolicy.APPEND_OR_REPLACE, trabajo
        )
    }

    // El backend registra el token FCM, no un ID de instalacion de Firebase.
    @Suppress("DEPRECATION")
    suspend fun registrar(context: Context): Boolean = sesion.withLock {
        if (!disponible(context)) return@withLock true
        val jwt = TokenManager(context).tokenCliente() ?: return@withLock true
        val mensaje = FirebaseMessaging.getInstance()
        if (!mensaje.isAutoInitEnabled) mensaje.isAutoInitEnabled = true
        val token = mensaje.token.esperar()
        if (token.isBlank()) return@withLock false
        val respuesta = RetrofitClient.usuarioApi.registrarFcm("Bearer $jwt", RegistroFcmRequest(token))
        respuesta.errorBody()?.close()
        respuesta.isSuccessful || (respuesta.code() in 400..499 && respuesta.code() !in setOf(408, 429))
    }

    @Suppress("DEPRECATION")
    suspend fun cerrarSesion(context: Context, tokenManager: TokenManager) {
        sesion.withLock {
            val jwt = tokenManager.tokenCliente()
            if (jwt != null && disponible(context)) {
                val retirado = withTimeoutOrNull(4_000) {
                    try {
                        val respuesta = RetrofitClient.usuarioApi.retirarFcm("Bearer $jwt")
                        respuesta.errorBody()?.close()
                        respuesta.isSuccessful
                    } catch (_: IOException) {
                        false
                    }
                } == true
                val mensaje = FirebaseMessaging.getInstance()
                mensaje.isAutoInitEnabled = false
                if (!retirado) withTimeoutOrNull(2_000) {
                    try {
                        mensaje.deleteToken().esperar()
                    } catch (_: IOException) {
                        // El cierre local no depende de la conexión.
                    }
                }
            }
            tokenManager.clearSession()
            if (disponible(context)) WorkManager.getInstance(context).cancelUniqueWork(TRABAJO_REGISTRO)
            NotificationManagerCompat.from(context).cancelAll()
        }
    }
}

class RegistroFcmWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val registrado = try {
            withTimeoutOrNull(12_000) { NotificacionesFcm.registrar(applicationContext) } == true
        } catch (_: IOException) {
            false
        }
        return when {
            registrado -> Result.success()
            runAttemptCount < 5 -> Result.retry()
            else -> Result.failure()
        }
    }
}

private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isActive) {
            if (task.isSuccessful) continuation.resume(task.result)
            else continuation.resumeWithException(IOException("No se pudo completar la operación FCM.", task.exception))
        }
    }
}
