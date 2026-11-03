package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.data.model.OrdenTrabajoDto
import com.tecsup.autosmart.data.network.OrdenTrabajoApi
import com.tecsup.autosmart.data.repository.OrdenTrabajoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import retrofit2.http.GET

class OrdenTrabajoTest {
    private val orden = OrdenTrabajoDto(81, 12, "AS-2026-00012", "EN_PROCESO",
        "2026-11-02T09:00:00", "Revisión", "ABC-123")
    private fun api(call: suspend () -> Response<List<OrdenTrabajoDto>>) = object : OrdenTrabajoApi {
        override suspend fun listar() = call()
    }

    @Test fun consultaEndpointRealSinConfundirIds() = runBlocking {
        val method = OrdenTrabajoApi::class.java.methods.single { it.name == "listar" }
        assertEquals("mechanics/me/work-orders", method.getAnnotation(GET::class.java)!!.value)
        val lista = OrdenTrabajoRepository(api { Response.success(listOf(orden)) }).listar().getOrThrow()
        assertEquals(81L, lista.single().idOrden)
        assertEquals(12L, lista.single().idCita)
    }

    @Test fun mapeaRespuestaRealSinInventarDatos() {
        val dto = Gson().fromJson("""{"idOrden":81,"idCita":12,"estado":"ESPERA_APROBACION","fechaInicio":"2026-11-02T09:00:00","placa":"ABC-123"}""", OrdenTrabajoDto::class.java)
        assertEquals(81L, dto.idOrden)
        assertEquals("ESPERA_APROBACION", dto.estado)
        assertNull(dto.servicio)
        assertNull(dto.ticket)
    }

    @Test fun distingueListaVaciaDeRespuestaNula() = runBlocking {
        assertTrue(OrdenTrabajoRepository(api { Response.success(emptyList()) }).listar().getOrThrow().isEmpty())
        assertTrue(OrdenTrabajoRepository(api { Response.success(null) }).listar().isFailure)
    }

    @Test fun rechazaOrdenesInvalidasDuplicadasOFinalizadas() = runBlocking {
        for (lista in listOf(listOf(orden.copy(idOrden = 0)), listOf(orden.copy(idCita = 0)),
            listOf(orden, orden), listOf(orden.copy(estado = "FINALIZADA")))) {
            assertTrue(OrdenTrabajoRepository(api { Response.success(lista) }).listar().isFailure)
        }
        assertTrue(OrdenTrabajoRepository(api { Response.success(listOf(
            orden.copy(estado = "ESPERA_APROBACION"))) }).listar().isSuccess)
    }

    @Test fun errorHttpNoSePresentaComoListaVacia() = runBlocking {
        for (code in listOf(401, 403, 500)) {
            assertTrue(OrdenTrabajoRepository(api { Response.error(code,
                """{"message":"Sin acceso"}""".toResponseBody()) }).listar().isFailure)
        }
    }

    @Test fun falloDeRedTieneMensajeComprensible() = runBlocking {
        val error = OrdenTrabajoRepository(api { throw java.io.IOException() }).listar().exceptionOrNull()
        assertTrue(error!!.message!!.contains("conexión"))
    }

    @Test fun propagaCancelacion() = runBlocking {
        try {
            OrdenTrabajoRepository(api { throw CancellationException("cancelada") }).listar()
            fail("Debe propagar CancellationException")
        } catch (_: CancellationException) { }
    }
    @Test fun consultaHttpRealDelListado() = runBlocking {
        val servidor = java.net.ServerSocket(0)
        val ejecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val solicitud = ejecutor.submit<String> {
                servidor.accept().use { socket ->
                    socket.soTimeout = 5000
                    val reader = socket.getInputStream().bufferedReader()
                    val primera = reader.readLine()
                    while (!reader.readLine().isNullOrEmpty()) { }
                    val body = Gson().toJson(listOf(orden)).toByteArray(Charsets.UTF_8)
                    socket.getOutputStream().apply {
                        write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray())
                        write(body)
                        flush()
                    }
                    primera
                }
            }
            val httpApi = retrofit2.Retrofit.Builder()
                .baseUrl("http://127.0.0.1:${servidor.localPort}/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build().create(OrdenTrabajoApi::class.java)
            assertEquals(listOf(orden), OrdenTrabajoRepository(httpApi).listar().getOrThrow())
            assertEquals("GET /mechanics/me/work-orders HTTP/1.1",
                solicitud.get(5, java.util.concurrent.TimeUnit.SECONDS))
        } finally {
            servidor.close()
            ejecutor.shutdownNow()
        }
    }

}
