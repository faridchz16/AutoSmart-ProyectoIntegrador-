package com.tecsup.autosmart.data.repository

import com.google.gson.JsonParser
import com.tecsup.autosmart.data.remote.MecanicoApi
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.mensajeDeError
import com.tecsup.autosmart.model.PerfilMecanicoDto
import com.tecsup.autosmart.model.PerfilMecanicoRequest
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Mismos límites que valida el backend (PerfilMecanicoRequest). */
const val MAX_ANIOS_EXPERIENCIA = 60
const val MAX_ESPECIALIDADES = 10
const val MAX_LARGO_ESPECIALIDAD = 60

class MecanicoRepository(private val api: MecanicoApi = RetrofitClient.mecanicoApi) {

    suspend fun perfil(): Result<PerfilMecanicoDto> = llamar("No se pudo cargar tu perfil. Revisa tu conexión.") {
        val r = api.perfil()
        if (!r.isSuccessful) throw Exception(errorDe(r.errorBody()?.string(), r.code()))
        r.body()?.takeIf { it.idUsuario > 0 } ?: throw Exception("El servidor no devolvió tu perfil.")
    }

    suspend fun guardar(anios: Int, especialidades: List<String>): Result<PerfilMecanicoDto> =
        llamar("No se pudo guardar tu perfil. Revisa tu conexión.") {
            val limpias = limpiar(especialidades)
            validar(anios, limpias)?.let { throw IllegalArgumentException(it) }
            val r = api.guardarPerfil(PerfilMecanicoRequest(anios, limpias))
            if (!r.isSuccessful) throw Exception(errorDe(r.errorBody()?.string(), r.code()))
            val body = r.body()
            require(body != null && body.aniosExperiencia == anios) { "El servidor no confirmó los cambios." }
            body
        }

    companion object {
        /** Quita espacios y repetidos (sin importar mayúsculas), conservando el primer texto. */
        fun limpiar(lista: List<String>): List<String> =
            lista.map { it.trim().replace(Regex("\\s+"), " ") }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }

        fun validar(anios: Int, especialidades: List<String>): String? = when {
            anios < 0 -> "Los años de experiencia no pueden ser negativos."
            anios > MAX_ANIOS_EXPERIENCIA -> "Los años de experiencia no pueden superar $MAX_ANIOS_EXPERIENCIA."
            especialidades.isEmpty() -> "Agrega al menos una especialidad."
            especialidades.size > MAX_ESPECIALIDADES -> "Puedes registrar hasta $MAX_ESPECIALIDADES especialidades."
            especialidades.any { it.length > MAX_LARGO_ESPECIALIDAD } ->
                "Cada especialidad puede tener hasta $MAX_LARGO_ESPECIALIDAD caracteres."
            else -> null
        }
    }

    private fun errorDe(cuerpo: String?, codigo: Int): String = runCatching {
        val json = JsonParser.parseString(cuerpo).asJsonObject
        listOf("error", "mensaje", "message").firstNotNullOfOrNull { clave ->
            json.get(clave)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
        }
    }.getOrNull() ?: mensajeDeError(cuerpo, codigo)

    private suspend fun <T> llamar(mensajeRed: String, bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(Exception(mensajeRed, e))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
