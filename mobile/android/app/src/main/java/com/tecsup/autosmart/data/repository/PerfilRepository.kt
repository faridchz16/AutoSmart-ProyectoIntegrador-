package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.model.PerfilResponse
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.UsuarioApi
import com.tecsup.autosmart.data.remote.mensajeDeError
import kotlinx.coroutines.CancellationException

class PerfilRepository(
    private val api: UsuarioApi = RetrofitClient.usuarioApi
) {
    suspend fun obtenerPerfil(): Result<PerfilResponse> = try {
        val response = api.obtenerPerfil()
        val perfil = response.body()
        when {
            !response.isSuccessful -> Result.failure(
                Exception(mensajeDeError(response.errorBody()?.string(), response.code()))
            )
            perfil == null || perfil.idUsuario <= 0 -> Result.failure(
                Exception("El servidor no devolvió los datos del perfil")
            )
            else -> Result.success(perfil)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión", e))
    }
}
