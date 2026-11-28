package com.tecsup.autosmart.ui.cliente

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File

/**
 * Fotos de los vehículos y del perfil guardadas SOLO en este celular.
 * El backend todavía no guarda fotos de vehículos; si se agrega, aquí se cambia por la API.
 */
object FotoVehiculoLocal {

    private fun carpeta(context: Context) = File(context.filesDir, "fotos_vehiculos").apply { mkdirs() }

    private fun archivo(context: Context, idVehiculo: Long) = File(carpeta(context), "vehiculo_$idVehiculo.jpg")

    private fun archivoPerfil(context: Context, idUsuario: Int) = File(carpeta(context), "perfil_$idUsuario.jpg")

    /** Guarda la foto del vehículo. */
    fun guardar(context: Context, idVehiculo: Long, uri: Uri): Boolean =
        guardarEn(context, archivo(context, idVehiculo), uri)

    /** Guarda la foto de perfil del usuario (también solo en este celular). */
    fun guardarPerfil(context: Context, idUsuario: Int, uri: Uri): Boolean =
        guardarEn(context, archivoPerfil(context, idUsuario), uri)

    /** Foto de perfil guardada o null si no eligió una. */
    fun cargarPerfil(context: Context, idUsuario: Int, maxLado: Int = 600): Bitmap? =
        cargarDe(archivoPerfil(context, idUsuario), maxLado)

    /** Guarda la imagen elegida reducida (máx. 1280 px) y con la orientación correcta. */
    private fun guardarEn(context: Context, destino: File, uri: Uri): Boolean = runCatching {
        val original = decodificarUri(context, uri, 1280) ?: return false
        val grados = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val final = if (grados == 0f) original else Bitmap.createBitmap(
            original, 0, 0, original.width, original.height, Matrix().apply { postRotate(grados) }, true
        )
        destino.outputStream().use { final.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        true
    }.getOrDefault(false)

    /** Devuelve la foto guardada o null si el vehículo aún no tiene foto. */
    fun cargar(context: Context, idVehiculo: Long, maxLado: Int = 900): Bitmap? =
        cargarDe(archivo(context, idVehiculo), maxLado)

    private fun cargarDe(f: File, maxLado: Int): Bitmap? {
        if (!f.isFile) return null
        return runCatching {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, limites)
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply {
                inSampleSize = escala(limites.outWidth, limites.outHeight, maxLado)
            })
        }.getOrNull()
    }

    private fun decodificarUri(context: Context, uri: Uri, maxLado: Int): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0) return null
        val opciones = BitmapFactory.Options().apply { inSampleSize = escala(limites.outWidth, limites.outHeight, maxLado) }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
    }

    private fun escala(ancho: Int, alto: Int, maxLado: Int): Int {
        var s = 1
        while (maxOf(ancho, alto) / (s * 2) >= maxLado) s *= 2
        return s
    }
}
