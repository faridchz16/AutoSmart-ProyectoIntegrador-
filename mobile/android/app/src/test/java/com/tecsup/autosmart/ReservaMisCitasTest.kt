package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.data.remote.CitaApi
import com.tecsup.autosmart.data.repository.CancelacionInciertaException
import com.tecsup.autosmart.data.repository.CitaRepository
import com.tecsup.autosmart.model.CitaCreadaResponse
import com.tecsup.autosmart.model.CitaRequest
import com.tecsup.autosmart.model.ReprogramarRequest
import com.tecsup.autosmart.model.SeguimientoDto
import com.tecsup.autosmart.viewmodel.esCancelable
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.math.BigDecimal
import java.time.LocalDateTime

// TEC2.6: pruebas funcionales de reserva (T26.2/T28.1), Mis citas (T30.2) y cancelación (T32.2)
class ReservaMisCitasTest {
    private val request = CitaRequest(1, 2, 3, "2026-11-20", "09:00")
    private val cita = CitaCreadaResponse(15, "AS-0015", "PENDIENTE", "2026-11-20", "09:00:00", "10:00:00",
        BigDecimal("80.00"), "Cambio de aceite", "Carlos Mendoza", "ABC-123")

    private fun api(
        crear: suspend (CitaRequest) -> Response<CitaCreadaResponse> = { Response.success(cita) },
        listar: suspend () -> Response<List<CitaCreadaResponse>> = { Response.success(listOf(cita)) },
        cancelar: suspend (Long) -> Response<CitaCreadaResponse> = { Response.success(cita.copy(estado = "CANCELADA")) }
    ) = object : CitaApi {
        override suspend fun reprogramarCita(idCita: Long, datos: ReprogramarRequest): Response<CitaCreadaResponse> = error("No usar")
        override suspend fun crearCita(request: CitaRequest) = crear(request)
        override suspend fun listarMisCitas(futuras: Boolean) = listar()
        override suspend fun seguimiento(idCita: Long): Response<SeguimientoDto> = error("No usar")
        override suspend fun cancelarCita(idCita: Long) = cancelar(idCita)
    }

    // ---- Reserva ----
    @Test fun reservaEnviaElJsonQueEsperaElBackend() {
        val json = Gson().toJsonTree(request).asJsonObject
        assertEquals(setOf("idServicio", "idMecanico", "idVehiculo", "fecha", "horaInicio"), json.keySet())
    }

    @Test fun reservaDevuelveElTicketDelServidor() = runBlocking {
        val repo = CitaRepository(api(crear = { assertEquals(request, it); Response.success(cita) }))
        assertEquals("AS-0015", repo.crearCita(request).getOrThrow().ticket)
    }

    @Test fun reservaFallaSiElTurnoYaNoEstaLibreOSinCuerpo() = runBlocking {
        assertTrue(CitaRepository(api(crear = { Response.error(409, "".toResponseBody()) })).crearCita(request).isFailure)
        assertTrue(CitaRepository(api(crear = { Response.success(null) })).crearCita(request).isFailure)
    }

    // ---- Mis citas ----
    @Test fun misCitasListaLasCitasDelCliente() = runBlocking {
        val lista = CitaRepository(api()).misCitas().getOrThrow()
        assertEquals(1, lista.size)
        assertEquals(15L, lista.first().idCita)
    }

    @Test fun misCitasVaciaYErrores() = runBlocking {
        assertTrue(CitaRepository(api(listar = { Response.success(emptyList()) })).misCitas().getOrThrow().isEmpty())
        assertTrue(CitaRepository(api(listar = { Response.success(null) })).misCitas().isFailure)
        assertTrue(CitaRepository(api(listar = { Response.error(401, "".toResponseBody()) })).misCitas().isFailure)
        assertTrue(CitaRepository(api(listar = { throw java.io.IOException("Sin red") })).misCitas().isFailure)
    }

    // ---- Cancelación ----
    @Test fun cancelarSoloEsExitoConEstadoCancelada() = runBlocking {
        assertEquals("CANCELADA", CitaRepository(api()).cancelarCita(15).getOrThrow().estado)
        val noConfirmada = CitaRepository(api(cancelar = { Response.success(cita) })).cancelarCita(15)
        assertTrue(noConfirmada.exceptionOrNull() is CancelacionInciertaException)
    }

    @Test fun cancelarErrorDelServidorOEnRedEsIncierto() = runBlocking {
        assertTrue(CitaRepository(api(cancelar = { Response.error(500, "".toResponseBody()) }))
            .cancelarCita(15).exceptionOrNull() is CancelacionInciertaException)
        assertTrue(CitaRepository(api(cancelar = { throw java.io.IOException("Sin red") }))
            .cancelarCita(15).exceptionOrNull() is CancelacionInciertaException)
        val rechazo = CitaRepository(api(cancelar = { Response.error(400, "".toResponseBody()) })).cancelarCita(15)
        assertTrue(rechazo.isFailure && rechazo.exceptionOrNull() !is CancelacionInciertaException)
    }

    @Test fun cancelarRequiereDosHorasDeAnticipacion() {
        val ahora = LocalDateTime.parse("2026-11-20T07:00:00")
        assertTrue(esCancelable(cita, ahora))
        assertFalse(esCancelable(cita, ahora.plusMinutes(1)))
        assertFalse(esCancelable(cita.copy(estado = "CANCELADA"), ahora))
        assertTrue(esCancelable(cita.copy(estado = "CONFIRMADA"), ahora))
    }
}
