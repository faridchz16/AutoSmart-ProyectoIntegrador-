package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.data.model.AgendaMecanicoDto
import com.tecsup.autosmart.data.model.CitaDetalleMecanicoDto
import com.tecsup.autosmart.data.network.AgendaMecanicoApi
import com.tecsup.autosmart.data.repository.AgendaMecanicoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AgendaMecanicoTest {
    private val agendaJson = """[{"idCita":12,"ticket":"T-12","fecha":"2026-11-02","horaInicio":"09:00","horaFin":"10:00","estado":"CONFIRMADA","servicio":"Revisión","cliente":"Cliente de prueba","placa":"ABC-123"}]"""
    private val detalleJson = """{"idCita":12,"ticket":"T-12","fecha":"2026-11-02","horaInicio":"09:00","horaFin":"10:00","estado":"CONFIRMADA","precio":120.50,"servicio":"Revisión","duracionMinutos":60,"cliente":"Cliente de prueba","telefonoCliente":null,"placa":"ABC-123","marca":"Toyota","modelo":"Yaris","anio":2020,"kilometraje":20000}"""
    private val cita = Gson().fromJson(agendaJson, Array<AgendaMecanicoDto>::class.java).single()
    private val detalle = Gson().fromJson(detalleJson, CitaDetalleMecanicoDto::class.java)
    private fun api(
        lista: suspend (String, String) -> Response<List<AgendaMecanicoDto>> = { _, _ -> Response.success(listOf(cita)) },
        ficha: suspend (Long) -> Response<CitaDetalleMecanicoDto> = { Response.success(detalle) }
    ) = object : AgendaMecanicoApi {
        override suspend fun listar(desde: String, hasta: String) = lista(desde, hasta)
        override suspend fun detalle(idCita: Long) = ficha(idCita)
    }

    @Test fun consultaUnDiaSinInventarRangosNiEstados() = runBlocking {
        val repo = AgendaMecanicoRepository(api(lista = { desde, hasta ->
            assertEquals("2026-11-02", desde)
            assertEquals(desde, hasta)
            Response.success(listOf(cita))
        }))
        assertEquals(listOf(cita), repo.listar("2026-11-02").getOrThrow())
    }

    @Test fun vacioEsDistintoDeError() = runBlocking {
        assertEquals(emptyList<AgendaMecanicoDto>(), AgendaMecanicoRepository(api(lista = { _, _ ->
            Response.success(emptyList())
        })).listar("2026-11-02").getOrThrow())
        assertTrue(AgendaMecanicoRepository(api(lista = { _, _ -> Response.success(null) }))
            .listar("2026-11-02").isFailure)
    }

    @Test fun rechazaIdsInvalidosODuplicados() = runBlocking {
        for (citas in listOf(listOf(cita.copy(idCita = 0)), listOf(cita, cita))) {
            assertTrue(AgendaMecanicoRepository(api(lista = { _, _ -> Response.success(citas) }))
                .listar("2026-11-02").isFailure)
        }
    }

    @Test fun detalleConservaLosCamposDelContratoYLosNulos() = runBlocking {
        val resultado = AgendaMecanicoRepository(api(ficha = { id ->
            assertEquals(12L, id)
            Response.success(detalle)
        })).detalle(12).getOrThrow()
        assertEquals("120.50", resultado.precio!!.toPlainString())
        assertNull(resultado.telefonoCliente)
        assertEquals(60, resultado.duracionMinutos)
        assertEquals("CONFIRMADA", resultado.estado)
        assertEquals("ABC-123", resultado.placa)
    }

    @Test fun noMuestraDetalleDeOtraCitaNiRespuestaNula() = runBlocking {
        assertTrue(AgendaMecanicoRepository(api()).detalle(99).isFailure)
        assertTrue(AgendaMecanicoRepository(api(ficha = { Response.success(null) })).detalle(12).isFailure)
    }

    @Test fun erroresDeSesionPermisosYServidorNoSonListaVacia() = runBlocking {
        for (codigo in listOf(400, 401, 403, 404, 500)) {
            val repo = AgendaMecanicoRepository(api(
                lista = { _, _ -> Response.error(codigo, "".toResponseBody()) },
                ficha = { Response.error(codigo, "".toResponseBody()) }))
            assertTrue(repo.listar("2026-11-02").isFailure)
            assertTrue(repo.detalle(12).isFailure)
        }
    }

    @Test fun propagaCancelacionesEnAmbasConsultas() = runBlocking {
        val causa = CancellationException("Cambio de pantalla")
        val repo = AgendaMecanicoRepository(api(lista = { _, _ -> throw causa }, ficha = { throw causa }))
        try { repo.listar("2026-11-02"); fail("Debe cancelar") }
        catch (e: CancellationException) { assertSame(causa, e) }
        try { repo.detalle(12); fail("Debe cancelar") }
        catch (e: CancellationException) { assertSame(causa, e) }
    }

    @Test fun fallosDeRedMantienenSuCausa() = runBlocking {
        val causa = java.io.IOException("Sin conexión")
        val repo = AgendaMecanicoRepository(api(lista = { _, _ -> throw causa }, ficha = { throw causa }))
        assertSame(causa, repo.listar("2026-11-02").exceptionOrNull()?.cause)
        assertSame(causa, repo.detalle(12).exceptionOrNull()?.cause)
    }

    @Test fun contratosHttpRealesDeAgendaYDetalle() = runBlocking {
        val servidor = ServerSocket(0)
        val ejecutor = Executors.newSingleThreadExecutor()
        try {
            val solicitudes = ejecutor.submit<List<String>> {
                (0..1).map { numero ->
                    servidor.accept().use { socket ->
                        socket.soTimeout = 5000
                        val reader = socket.getInputStream().bufferedReader()
                        val primera = reader.readLine()
                        while (!reader.readLine().isNullOrEmpty()) { }
                        val body = (if (numero == 0) agendaJson else detalleJson).toByteArray(Charsets.UTF_8)
                        socket.getOutputStream().apply {
                            write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray())
                            write(body)
                            flush()
                        }
                        primera
                    }
                }
            }
            val api = Retrofit.Builder().baseUrl("http://127.0.0.1:${servidor.localPort}/")
                .addConverterFactory(GsonConverterFactory.create()).build().create(AgendaMecanicoApi::class.java)
            val repo = AgendaMecanicoRepository(api)
            assertEquals(listOf(cita), repo.listar("2026-11-02").getOrThrow())
            assertEquals(detalle, repo.detalle(12).getOrThrow())
            assertEquals(listOf("GET /mechanics/me/appointments?desde=2026-11-02&hasta=2026-11-02 HTTP/1.1",
                "GET /appointments/12 HTTP/1.1"), solicitudes.get(5, TimeUnit.SECONDS))
        } finally {
            servidor.close()
            ejecutor.shutdownNow()
        }
    }
}
