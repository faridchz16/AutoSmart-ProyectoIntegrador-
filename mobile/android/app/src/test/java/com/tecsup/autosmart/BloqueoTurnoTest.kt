package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.data.remote.ReservaApi
import com.tecsup.autosmart.data.repository.BloqueoTurnoRepository
import com.tecsup.autosmart.data.repository.bloqueoValido
import com.tecsup.autosmart.model.BloqueoRequest
import com.tecsup.autosmart.model.BloqueoResponse
import com.tecsup.autosmart.model.MecanicoDto
import com.tecsup.autosmart.model.TurnoDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

// T33.2: bloqueo temporal del turno (POST y DELETE /appointments/holds)
class BloqueoTurnoTest {
    private val datos = BloqueoRequest(1, 2, "2026-11-20", "09:00")
    private val bloqueo = BloqueoResponse(7, "2026-11-20", "09:00", "10:00", "2026-11-12T10:10", 600)

    private fun api(
        bloquear: suspend (BloqueoRequest) -> Response<BloqueoResponse> = { Response.success(bloqueo) },
        liberar: suspend (Long) -> Response<Unit> = { Response.success(204, Unit) }
    ) = object : ReservaApi {
        override suspend fun mecanicosPorServicio(idServicio: Long): Response<List<MecanicoDto>> = error("No usar")
        override suspend fun turnos(idServicio: Long, fecha: String, idMecanico: Int): Response<List<TurnoDto>> = error("No usar")
        override suspend fun bloquearTurno(datos: BloqueoRequest) = bloquear(datos)
        override suspend fun liberarTurno(idBloqueo: Long) = liberar(idBloqueo)
    }

    @Test fun jsonEnviaLosCamposQuePideElBackend() {
        val json = Gson().toJsonTree(datos).asJsonObject
        assertEquals(setOf("idServicio", "idMecanico", "fecha", "horaInicio"), json.keySet())
        assertEquals("09:00", json.get("horaInicio").asString)
    }

    @Test fun bloqueaYDevuelveElTiempoDelServidor() = runBlocking {
        val repo = BloqueoTurnoRepository(api(bloquear = { assertEquals(datos, it); Response.success(bloqueo) }))
        val resultado = repo.bloquear(datos).getOrThrow()
        assertEquals(7L, resultado.idBloqueo)
        assertEquals(600L, resultado.segundosRestantes)
    }

    @Test fun turnoOcupadoDevuelveError() = runBlocking {
        val repo = BloqueoTurnoRepository(api(bloquear = { Response.error(409, "".toResponseBody()) }))
        assertTrue(repo.bloquear(datos).isFailure)
    }

    @Test fun respuestaInvalidaNoSeAcepta() = runBlocking {
        assertTrue(BloqueoTurnoRepository(api(bloquear = { Response.success(null) })).bloquear(datos).isFailure)
        assertTrue(BloqueoTurnoRepository(api(bloquear = { Response.success(bloqueo.copy(idBloqueo = 0)) })).bloquear(datos).isFailure)
        assertTrue(BloqueoTurnoRepository(api(bloquear = { Response.success(bloqueo.copy(segundosRestantes = 0)) })).bloquear(datos).isFailure)
        assertTrue(BloqueoTurnoRepository(api(bloquear = { Response.success(bloqueo.copy(horaInicio = "10:00")) })).bloquear(datos).isFailure)
    }

    @Test fun horaConSegundosDelServidorEsValida() {
        assertTrue(bloqueoValido(bloqueo.copy(horaInicio = "09:00:00"), datos))
    }

    @Test fun liberarEnviaElIdYAceptaBloqueoVencido() = runBlocking {
        var liberado = -1L
        assertTrue(BloqueoTurnoRepository(api(liberar = { liberado = it; Response.success(204, Unit) })).liberar(7).isSuccess)
        assertEquals(7L, liberado)
        assertTrue(BloqueoTurnoRepository(api(liberar = { Response.error(404, "".toResponseBody()) })).liberar(7).isSuccess)
        assertTrue(BloqueoTurnoRepository(api(liberar = { Response.error(403, "".toResponseBody()) })).liberar(7).isFailure)
    }

    @Test fun errorDeRedYCancelacion() = runBlocking {
        val causa = java.io.IOException("Sin conexión")
        assertSame(causa, BloqueoTurnoRepository(api(bloquear = { throw causa })).bloquear(datos).exceptionOrNull()?.cause)
        val cancelacion = CancellationException("Salir")
        try {
            BloqueoTurnoRepository(api(bloquear = { throw cancelacion })).bloquear(datos)
            fail("Debe propagar la cancelación")
        } catch (e: CancellationException) {
            assertSame(cancelacion, e)
        }
    }
}
