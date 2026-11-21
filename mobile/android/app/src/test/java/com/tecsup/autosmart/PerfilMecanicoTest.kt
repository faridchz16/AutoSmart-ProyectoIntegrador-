package com.tecsup.autosmart

import com.tecsup.autosmart.data.remote.MecanicoApi
import com.tecsup.autosmart.data.repository.MecanicoRepository
import com.tecsup.autosmart.model.PerfilMecanicoDto
import com.tecsup.autosmart.model.PerfilMecanicoRequest
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT

/** Perfil profesional del mecánico en la app (GET/PUT /mechanics/me). */
class PerfilMecanicoTest {
    private val juan = PerfilMecanicoDto(8, "Juan Rodríguez Quintero", "juan.rodriguez@autosmart.com", 0, emptyList())

    @Test
    fun usaLosEndpointsDelBackend() {
        fun ruta(nombre: String) = MecanicoApi::class.java.methods.single { it.name == nombre }
        assertEquals("mechanics/me", ruta("perfil").getAnnotation(GET::class.java)?.value)
        assertEquals("mechanics/me", ruta("guardarPerfil").getAnnotation(PUT::class.java)?.value)
    }

    @Test
    fun limpiaEspaciosYRepetidosSinImportarMayusculas() {
        assertEquals(listOf("Frenos", "Motor diésel"),
            MecanicoRepository.limpiar(listOf("  Frenos ", "frenos", "Motor   diésel", " ")))
    }

    @Test
    fun validaLosMismosLimitesQueElBackend() {
        assertNull(MecanicoRepository.validar(5, listOf("Frenos")))
        assertTrue(MecanicoRepository.validar(-1, listOf("Frenos")) != null)
        assertTrue(MecanicoRepository.validar(61, listOf("Frenos")) != null)
        assertTrue(MecanicoRepository.validar(5, emptyList()) != null)
        assertTrue(MecanicoRepository.validar(5, List(11) { "E$it" }) != null)
        assertTrue(MecanicoRepository.validar(5, listOf("x".repeat(61))) != null)
    }

    @Test
    fun guardaElPerfilLimpioYDevuelveLoConfirmado() = runBlocking {
        var enviado: PerfilMecanicoRequest? = null
        val repo = repositorio(guardar = { req ->
            enviado = req
            Response.success(juan.copy(aniosExperiencia = req.aniosExperiencia, especialidades = req.especialidades))
        })
        val r = repo.guardar(6, listOf(" Frenos", "FRENOS", "Suspensión")).getOrThrow()
        assertEquals(PerfilMecanicoRequest(6, listOf("Frenos", "Suspensión")), enviado)
        assertEquals(listOf("Frenos", "Suspensión"), r.especialidades)
    }

    @Test
    fun noEnviaDatosInvalidosYMuestraElErrorDelServidor() = runBlocking {
        assertTrue(repositorio(guardar = { error("No debe enviar") }).guardar(5, emptyList()).isFailure)
        val r = repositorio(guardar = { Response.error(400, "{\"error\":\"Las especialidades no pueden estar vacias\"}".toResponseBody()) })
            .guardar(5, listOf("Frenos"))
        assertEquals("Las especialidades no pueden estar vacias", r.exceptionOrNull()?.message)
    }

    private fun repositorio(
        perfil: suspend () -> Response<PerfilMecanicoDto> = { Response.success(juan) },
        guardar: suspend (PerfilMecanicoRequest) -> Response<PerfilMecanicoDto>
    ) = MecanicoRepository(object : MecanicoApi {
        override suspend fun perfil() = perfil()
        override suspend fun guardarPerfil(request: PerfilMecanicoRequest) = guardar(request)
    })
}
