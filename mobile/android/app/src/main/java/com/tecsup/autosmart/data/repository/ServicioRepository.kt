package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.Servicio
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.data.network.mensajeDeError
import kotlin.coroutines.cancellation.CancellationException

class ServicioRepository {

    suspend fun listar(q: String): Result<List<Servicio>> = try {
        val res = RetrofitClient.servicioApi.listar(q.ifBlank { null })
        val body = res.body()
        if (res.isSuccessful && body != null) Result.success(body)
        else Result.failure(Exception(mensajeDeError(res.errorBody()?.string(), res.code())))
    } catch (e: CancellationException) {
        throw e   // una búsqueda anterior cancelada no es un error
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión"))
    }

    suspend fun detalle(id: Long): Result<Servicio> = try {
        val res = RetrofitClient.servicioApi.detalle(id)
        val body = res.body()
        if (res.isSuccessful && body != null) Result.success(body)
        else Result.failure(Exception(mensajeDeError(res.errorBody()?.string(), res.code())))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión"))
    }
}