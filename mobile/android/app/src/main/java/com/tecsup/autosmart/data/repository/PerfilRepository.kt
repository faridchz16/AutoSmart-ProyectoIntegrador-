package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.PerfilResponse
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.data.network.UsuarioApi
import com.tecsup.autosmart.data.network.mensajeDeError
import com.tecsup.autosmart.data.model.PerfilRequest
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

    suspend fun actualizarPerfil(idUsuario: Int, datos: PerfilRequest): Result<PerfilResponse> = try {
        val respuesta = api.actualizarPerfil(datos)
        val perfil = respuesta.body()
        when {
            !respuesta.isSuccessful -> Result.failure(Exception(
                mensajeDeError(respuesta.errorBody()?.string(), respuesta.code())))
            perfil == null || idUsuario <= 0 || perfil.idUsuario != idUsuario ->
                Result.failure(Exception("No se pudo verificar el perfil actualizado. Consulta tu perfil antes de reintentar."))
            else -> Result.success(perfil)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo confirmar la actualización. Revisa tu conexión; tus datos ingresados se conservaron.", e))
    }

}
