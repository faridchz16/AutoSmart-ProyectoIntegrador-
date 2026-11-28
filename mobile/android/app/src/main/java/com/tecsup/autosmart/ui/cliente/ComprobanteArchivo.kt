package com.tecsup.autosmart.ui.cliente

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.tecsup.autosmart.data.repository.ComprobantePdf
import java.io.File

/**
 * T64.2: guarda el comprobante en la carpeta Descargas del celular y lo abre con el visor de PDF.
 * En Android 10+ usa MediaStore (no pide permisos); en versiones anteriores, la carpeta de la app.
 */
object ComprobanteArchivo {

    /** Devuelve un texto para mostrar al cliente. */
    fun guardarYAbrir(context: Context, pdf: ComprobantePdf): String {
        val uri = guardar(context, pdf) ?: return "No se pudo guardar el comprobante en el celular."
        val abrir = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(abrir)
            "Comprobante guardado en Descargas: ${pdf.nombre}"
        } catch (_: ActivityNotFoundException) {
            "Comprobante guardado en Descargas: ${pdf.nombre}. Instala un visor de PDF para abrirlo."
        }
    }

    private fun guardar(context: Context, pdf: ComprobantePdf): Uri? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valores = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, pdf.nombre)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores) ?: return null
            resolver.openOutputStream(uri)?.use { it.write(pdf.bytes) }
            valores.clear()
            valores.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, valores, null, null)
            uri
        } else {
            val carpeta = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "").apply { mkdirs() }
            val archivo = File(carpeta, pdf.nombre)
            archivo.writeBytes(pdf.bytes)
            FileProvider.getUriForFile(context, "${context.packageName}.fotos", archivo)
        }
    }.getOrNull()
}

/** Recuerda en el celular qué servicios ya calificó el cliente, para no volver a pedirlo. */
object CalificacionesLocales {
    private const val ARCHIVO = "calificaciones"

    fun contiene(context: Context, idOrden: Long): Boolean =
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getBoolean("orden_$idOrden", false)

    fun guardar(context: Context, idOrden: Long) {
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putBoolean("orden_$idOrden", true).apply()
    }
}
