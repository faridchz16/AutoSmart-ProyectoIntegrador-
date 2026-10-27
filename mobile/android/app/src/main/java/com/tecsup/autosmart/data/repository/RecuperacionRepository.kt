package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.ForgotPasswordRequest
import com.tecsup.autosmart.data.model.ResetPasswordRequest
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.data.network.mensajeDeError
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

class RecuperacionRepository {

    suspend fun solicitarCodigo(correo: String): Result<Unit> =
        llamar { RetrofitClient.authApi.olvideContrasena(ForgotPasswordRequest(correo)) }

    suspend fun restablecer(correo: String, codigo: String, nuevaPassword: String): Result<Unit> =
        llamar {
            RetrofitClient.authApi.restablecerContrasena(
                ResetPasswordRequest(correo, codigo, nuevaPassword)
            )
        }

    private suspend fun llamar(bloque: suspend () -> Response<Unit>): Result<Unit> = try {
        val res = bloque()
        if (res.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(mensajeDeError(res.errorBody()?.string(), res.code())))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión"))
    }
}