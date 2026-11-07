package com.tecsup.autosmart.data.repository

import com.google.gson.JsonParser
import com.tecsup.autosmart.model.*
import com.tecsup.autosmart.data.remote.ChecklistApi
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.mensajeDeError
import kotlinx.coroutines.CancellationException

class ChecklistRepository(private val api: ChecklistApi = RetrofitClient.checklistApi) {
    suspend fun obtener(idOrden: Long): Result<ChecklistResponse?> = try {
        require(idOrden > 0) { "La orden no es válida." }
        val response = api.obtener(idOrden)
        val error = response.errorBody()?.string()
        val sinChecklist = response.code() == 404 && runCatching {
            val json = JsonParser.parseString(error).asJsonObject
            listOf("message", "mensaje", "error").any {
                json.get(it)?.takeIf { value -> value.isJsonPrimitive }?.asString == "La orden aun no tiene checklist"
            }
        }.getOrDefault(false)
        when {
            sinChecklist -> Result.success(null)
            !response.isSuccessful -> Result.failure(Exception(mensajeDeError(error, response.code())))
            else -> Result.success(validarRespuesta(response.body(), idOrden))
        }
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { Result.failure(Exception(e.message ?: "No se pudo consultar el checklist.", e)) }

    suspend fun guardar(idOrden: Long, request: ChecklistRequest): Result<ChecklistResponse> = try {
        require(idOrden > 0) { "La orden no es válida." }
        validarChecklist(request)?.let { throw IllegalArgumentException(it) }
        val response = api.guardar(idOrden, request)
        if (!response.isSuccessful) Result.failure(Exception(mensajeDeError(response.errorBody()?.string(), response.code())))
        else {
            val body = validarRespuesta(response.body(), idOrden)
            require(body.items!!.associate { it.elemento to (it.estado to it.observacion?.trim().orEmpty()) } ==
                request.items.associate { it.elemento to (it.estado to it.observacion?.trim().orEmpty()) } &&
                body.observacionGeneral?.trim().orEmpty() == request.observacionGeneral?.trim().orEmpty()) {
                "La respuesta no confirma los datos enviados. Consulta el checklist antes de continuar."
            }
            Result.success(body)
        }
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { Result.failure(Exception(e.message ?: "No se pudo guardar el checklist.", e)) }

    private fun validarRespuesta(body: ChecklistResponse?, idOrden: Long): ChecklistResponse {
        require(body != null && body.idOrden == idOrden && body.idChecklist > 0 &&
            !body.fechaRegistro.isNullOrBlank() && body.items != null &&
            validarChecklist(ChecklistRequest(body.observacionGeneral, body.items)) == null) {
            "El servidor no devolvió un checklist válido para esta orden."
        }
        return body
    }
}
