package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.model.*
import com.tecsup.autosmart.data.remote.DeficienciaApi
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.mensajeDeError
import kotlinx.coroutines.CancellationException

class RegistroDeficienciaInciertoException(message: String, cause: Throwable? = null) : Exception(message, cause)

class DeficienciaRepository(private val api: DeficienciaApi = RetrofitClient.deficienciaApi) {
    suspend fun listar(idOrden: Long): Result<List<DeficienciaResponse>> = try {
        require(idOrden > 0) { "La orden no es válida." }
        val response = api.listar(idOrden)
        val lista = response.body()
        when {
            !response.isSuccessful -> Result.failure(Exception(mensajeDeError(response.errorBody()?.string(), response.code())))
            lista == null || lista.any { !valida(it, idOrden) } || lista.distinctBy { it.idDeficiencia }.size != lista.size ->
                Result.failure(Exception("El servidor no devolvió una lista válida para esta orden."))
            else -> Result.success(lista)
        }
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { Result.failure(Exception("No se pudieron consultar las deficiencias. Revisa tu conexión.", e)) }

    suspend fun registrar(idOrden: Long, request: DeficienciaRequest): Result<DeficienciaResponse> {
        if (idOrden <= 0) return Result.failure(Exception("La orden no es válida."))
        validarDeficiencia(request)?.let { return Result.failure(Exception(it)) }
        return try {
            val response = api.registrar(idOrden, request)
            val body = response.body()
            when {
                response.code() == 408 || response.code() >= 500 -> Result.failure(RegistroDeficienciaInciertoException(
                    "No se pudo confirmar el registro. Comprueba la lista antes de volver a enviarlo."))
                !response.isSuccessful -> Result.failure(Exception(mensajeDeError(response.errorBody()?.string(), response.code())))
                body == null || !valida(body, idOrden) || !coincide(body, request) ->
                    Result.failure(RegistroDeficienciaInciertoException("La respuesta no confirma el registro. Comprueba la lista antes de continuar."))
                else -> Result.success(body)
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(RegistroDeficienciaInciertoException(
            "Se perdió la conexión. Comprueba si la deficiencia se registró antes de reenviarla.", e)) }
    }

    private fun valida(body: DeficienciaResponse, idOrden: Long) = body.idDeficiencia > 0 && body.idOrden == idOrden &&
        !body.fechaRegistro.isNullOrBlank() && body.categoria in categoriasDeficiencia &&
        body.severidad in severidadesDeficiencia && !body.descripcion.isNullOrBlank() && body.descripcion.length <= 500
}

internal fun coincide(body: DeficienciaResponse, request: DeficienciaRequest) = body.categoria == request.categoria &&
    body.severidad == request.severidad && body.descripcion?.trim() == request.descripcion.trim()
