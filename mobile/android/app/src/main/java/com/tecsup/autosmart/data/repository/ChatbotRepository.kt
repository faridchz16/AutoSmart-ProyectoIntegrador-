package com.tecsup.autosmart.data.repository

import com.google.gson.JsonParser
import com.tecsup.autosmart.data.remote.ChatbotApi
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.mensajeDeError
import com.tecsup.autosmart.model.ChatbotRequest
import com.tecsup.autosmart.model.ChatbotResponse
import kotlinx.coroutines.CancellationException
import java.io.IOException

class ChatbotRepository(private val api: ChatbotApi = RetrofitClient.chatbotApi) {

    /** Envía el mensaje (máx. 500 caracteres) manteniendo la misma conversación. */
    suspend fun enviar(mensaje: String, idConversacion: String?): Result<ChatbotResponse> = try {
        val texto = mensaje.trim().take(500)
        require(texto.isNotEmpty()) { "Escribe un mensaje." }
        val r = api.enviar(ChatbotRequest(texto, idConversacion))
        if (!r.isSuccessful) {
            Result.failure(Exception(errorDe(r.errorBody()?.string(), r.code())))
        } else {
            val body = r.body()
            require(body != null && !body.respuesta.isNullOrBlank()) { "El asistente no devolvió una respuesta." }
            Result.success(body)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(Exception("No me pude conectar con el taller. Revisa tu conexión.", e))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun errorDe(cuerpo: String?, codigo: Int): String = runCatching {
        val json = JsonParser.parseString(cuerpo).asJsonObject
        listOf("error", "mensaje", "message").firstNotNullOfOrNull { clave ->
            json.get(clave)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
        }
    }.getOrNull() ?: mensajeDeError(cuerpo, codigo)
}
