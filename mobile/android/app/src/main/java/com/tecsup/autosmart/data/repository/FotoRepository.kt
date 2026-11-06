package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.*
import com.tecsup.autosmart.data.network.FotoApi
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.data.network.mensajeDeError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.ByteArrayOutputStream

const val MAXIMO_FOTOS_DEFICIENCIA = 5
const val MAXIMO_BYTES_FOTO = 5L * 1024 * 1024
class SubidaFotoInciertaException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal fun tipoFoto(bytes: ByteArray): String? = when {
    bytes.size >= 3 && (bytes[0].toInt() and 255) == 255 && (bytes[1].toInt() and 255) == 216 &&
        (bytes[2].toInt() and 255) == 255 -> "image/jpeg"
    bytes.size >= 8 && bytes.take(8) == listOf(137,80,78,71,13,10,26,10).map { it.toByte() } -> "image/png"
    else -> null
}

internal fun validarArchivoFoto(file: File): String {
    require(file.isFile && file.length() > 0) { "La captura está vacía o ya no está disponible. Vuelve a tomarla." }
    require(file.length() <= MAXIMO_BYTES_FOTO) { "La foto no puede superar 5 MB. Vuelve a tomarla con menor resolución." }
    val header = ByteArray(8)
    val count = file.inputStream().use { it.read(header) }
    return requireNotNull(tipoFoto(header.copyOf(count.coerceAtLeast(0)))) { "Solo se permiten fotos JPG o PNG." }
}

class FotoRepository(private val api: FotoApi = RetrofitClient.fotoApi,
                     private val deficiencias: DeficienciaRepository = DeficienciaRepository()) {
    suspend fun listar(idOrden: Long, idDeficiencia: Long): Result<List<FotoReferenciaDto>> {
        val resultado = deficiencias.listar(idOrden)
        return resultado.mapCatching { lista ->
            val deficiencia = lista.singleOrNull { it.idDeficiencia == idDeficiencia }
                ?: throw Exception("La deficiencia no existe en esta orden.")
            val fotos = deficiencia.fotos ?: throw Exception("El servidor no devolvió las fotos de esta deficiencia.")
            require(fotos.all { it.idFoto > 0 && it.url == "/photos/${it.idFoto}" } &&
                fotos.distinctBy { it.idFoto }.size == fotos.size) { "La lista de fotos no es válida." }
            fotos
        }
    }

    suspend fun subir(idOrden: Long, idDeficiencia: Long, file: File): Result<FotoCreadaResponse> = withContext(Dispatchers.IO) {
        if (idOrden <= 0 || idDeficiencia <= 0) return@withContext Result.failure(Exception("La orden o deficiencia no es válida."))
        val tipo = try { validarArchivoFoto(file) } catch (e: Exception) { return@withContext Result.failure(e) }
        try {
            val part = MultipartBody.Part.createFormData("foto", if (tipo == "image/png") "evidencia.png" else "evidencia.jpg",
                file.asRequestBody(tipo.toMediaType()))
            val response = api.subir(idOrden, idDeficiencia, part)
            val body = response.body()
            when {
                response.code() == 408 || response.code() >= 500 -> Result.failure(SubidaFotoInciertaException(
                    "No se pudo confirmar la subida. Consulta las fotos antes de volver a enviar."))
                !response.isSuccessful -> Result.failure(Exception(mensajeDeError(response.errorBody()?.string(), response.code())))
                body == null || body.idFoto <= 0 || body.idDeficiencia != idDeficiencia || body.url != "/photos/${body.idFoto}" ->
                    Result.failure(SubidaFotoInciertaException("La respuesta no confirma la foto. Consulta las fotos antes de continuar."))
                else -> Result.success(body)
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(SubidaFotoInciertaException("Se perdió la conexión. Comprueba si la foto ya se guardó.", e)) }
    }

    suspend fun obtener(idFoto: Long): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            require(idFoto > 0) { "La foto no es válida." }
            val response = api.obtener(idFoto)
            if (!response.isSuccessful) {
                response.body()?.close()
                Result.failure(Exception(mensajeDeError(response.errorBody()?.string(), response.code())))
            } else {
                val body = requireNotNull(response.body()) { "La foto no está disponible." }
                val bytes = body.use {
                    require(it.contentLength() <= MAXIMO_BYTES_FOTO) { "La imagen supera el tamaño permitido." }
                    val buffer = ByteArray(8192)
                    val output = ByteArrayOutputStream()
                    it.byteStream().use { input ->
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size().toLong() + count <= MAXIMO_BYTES_FOTO) { "La imagen supera el tamaño permitido." }
                            output.write(buffer, 0, count)
                        }
                    }
                    output.toByteArray()
                }
                require(tipoFoto(bytes) != null) { "El servidor no devolvió una imagen JPG o PNG." }
                Result.success(bytes)
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(Exception("No se pudo cargar la foto. Revisa tu conexión.", e)) }
    }
}
