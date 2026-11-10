package com.tecsup.autosmart.data.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tecsup.autosmart.MainActivity
import com.tecsup.autosmart.data.local.TokenManager
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class AutoSmartMessagingService : FirebaseMessagingService() {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        if (token.isNotBlank()) NotificacionesFcm.programarRegistro(applicationContext)
    }

    @SuppressLint("MissingPermission")
    override fun onMessageReceived(message: RemoteMessage) {
        val idCita = citaDeAviso(message.data["tipo"], message.data["idCita"]) ?: return
        val cliente = try {
            runBlocking {
                withTimeoutOrNull(1_500) { TokenManager(applicationContext).tokenCliente() } != null
            }
        } catch (_: IOException) {
            false
        }
        if (!cliente) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        NotificacionesFcm.crearCanal(this)
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("tipo", TIPO_ORDEN_FINALIZADA)
            .putExtra("idCita", idCita.toString())
        val abrir = PendingIntent.getActivity(this, idCita.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notificacion = NotificationCompat.Builder(this, CANAL_VEHICULO_LISTO)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Tu vehículo está listo")
            .setContentText("Toca para ver el seguimiento de tu cita.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(idCita.hashCode(), notificacion)
    }
}
