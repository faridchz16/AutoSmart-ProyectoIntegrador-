package com.tecsup.autosmart

import com.tecsup.autosmart.model.*
import com.tecsup.autosmart.data.remote.RegistroFcmRequest
import com.tecsup.autosmart.data.remote.UsuarioApi
import com.tecsup.autosmart.data.repository.PerfilRepository
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class PerfilTest {
    private val perfil = PerfilResponse(9, "Nombre", "correo@example.invalid", null, "CLIENTE")
    private val datos = PerfilRequest("Nuevo nombre", "+51987654321")
    private fun api(responder: suspend (PerfilRequest) -> Response<PerfilResponse>) = object : UsuarioApi {
        override suspend fun obtenerPerfil() = Response.success(perfil)
        override suspend fun actualizarPerfil(datos: PerfilRequest) = responder(datos)
        override suspend fun registrarFcm(authorization: String, request: RegistroFcmRequest): Response<Unit> = Response.success(Unit)
        override suspend fun retirarFcm(authorization: String): Response<Unit> = Response.success(Unit)
    }
    @Test fun nombreObligatorioYLimite100() {
        assertNotNull(validarPerfil(" ", ""))
        assertNotNull(validarPerfil("a".repeat(101), ""))
        assertNull(validarPerfil("a".repeat(100), ""))
    }
    @Test fun telefonoOpcionalConDigitosYMasInicial() {
        for (t in listOf("", "1234567", "+51987654321", "1".repeat(15))) assertNull(t, validarPerfil("Nombre", t))
        for (t in listOf("123", "abc", "12+3456789", "1".repeat(16), "987 654 321")) assertNotNull(t, validarPerfil("Nombre", t))
    }
    @Test fun jsonNoEnviaCorreoRolNiId() {
        val json = Gson().toJsonTree(datos).asJsonObject
        assertEquals(setOf("nombre", "telefono"), json.keySet())
    }
    @Test fun actualizaYDevuelveRespuestaReal() = runBlocking {
        val nuevo = perfil.copy(nombre = datos.nombre, telefono = datos.telefono)
        val repo = PerfilRepository(api { assertEquals(datos, it); Response.success(nuevo) })
        assertEquals(nuevo, repo.actualizarPerfil(9, datos).getOrThrow())
    }
    @Test fun telefonoVacioSeEnviaVacioYRespuestaNulaSeConserva() = runBlocking {
        val repo = PerfilRepository(api { assertEquals("", it.telefono); Response.success(perfil) })
        assertNull(repo.actualizarPerfil(9, PerfilRequest("Nombre", "")).getOrThrow().telefono)
    }
    @Test fun noAceptaOtroUsuarioNiCuerpoNulo() = runBlocking {
        assertTrue(PerfilRepository(api { Response.success(perfil.copy(idUsuario = 10)) }).actualizarPerfil(9, datos).isFailure)
        assertTrue(PerfilRepository(api { Response.success(null) }).actualizarPerfil(9, datos).isFailure)
    }
    @Test fun erroresHttpNoSePresentanComoExito() = runBlocking {
        for (code in listOf(400, 401, 403, 500)) {
            assertTrue(PerfilRepository(api { Response.error(code, "".toResponseBody()) }).actualizarPerfil(9, datos).isFailure)
        }
    }
    @Test fun redConservaCausaYCancelacionSePropaga() = runBlocking {
        val causa = java.io.IOException("Sin conexión")
        assertSame(causa, PerfilRepository(api { throw causa }).actualizarPerfil(9, datos).exceptionOrNull()?.cause)
        val cancelacion = CancellationException("Salir")
        try { PerfilRepository(api { throw cancelacion }).actualizarPerfil(9, datos); fail("Debe cancelar") }
        catch (e: CancellationException) { assertSame(cancelacion, e) }
    }
}
