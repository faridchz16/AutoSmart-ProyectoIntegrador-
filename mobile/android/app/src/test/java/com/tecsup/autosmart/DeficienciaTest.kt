package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.data.model.*
import com.tecsup.autosmart.data.network.DeficienciaApi
import com.tecsup.autosmart.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class DeficienciaTest {
    private val request = DeficienciaRequest("FRENOS", "CRITICA", "Pastillas desgastadas")
    private val body = DeficienciaResponse(24, 81, "FRENOS", "CRITICA", "Pastillas desgastadas", "2026-11-05T10:00:00")
    private fun api(
        get: suspend (Long) -> Response<List<DeficienciaResponse>> = { Response.success(listOf(body)) },
        post: suspend (Long, DeficienciaRequest) -> Response<DeficienciaResponse> = { _, _ -> Response.success(201, body) }
    ) = object : DeficienciaApi {
        override suspend fun listar(idOrden: Long) = get(idOrden)
        override suspend fun registrar(idOrden: Long, request: DeficienciaRequest) = post(idOrden, request)
    }

    @Test fun categoriasYSeveridadesCoincidenConContrato() {
        assertEquals(listOf("MOTOR", "FRENOS", "SUSPENSION", "ELECTRICO", "NEUMATICOS", "CARROCERIA", "FLUIDOS", "OTRO"), categoriasDeficiencia)
        assertEquals(listOf("LEVE", "MODERADA", "CRITICA"), severidadesDeficiencia)
        assertNull(validarDeficiencia(request))
        assertNotNull(validarDeficiencia(request.copy(categoria = "")))
        assertNotNull(validarDeficiencia(request.copy(severidad = "")))
    }

    @Test fun descripcionObligatoriaYMaximo500() {
        assertNotNull(validarDeficiencia(request.copy(descripcion = "  ")))
        assertNull(validarDeficiencia(request.copy(descripcion = "a".repeat(500))))
        assertNotNull(validarDeficiencia(request.copy(descripcion = "a".repeat(501))))
    }

    @Test fun requestSoloIncluyeCamposAutorizados() {
        assertEquals(setOf("categoria", "severidad", "descripcion"), Gson().toJsonTree(request).asJsonObject.keySet())
    }

    @Test fun listaVaciaNoEsErrorNiDatosInventados() = runBlocking {
        assertTrue(DeficienciaRepository(api(get = { Response.success(emptyList()) })).listar(81).getOrThrow().isEmpty())
        assertTrue(DeficienciaRepository(api(get = { Response.success(null) })).listar(81).isFailure)
    }

    @Test fun rechazaIdsAjenosDuplicadosYDatosInvalidos() = runBlocking {
        for (list in listOf(listOf(body.copy(idOrden = 12)), listOf(body.copy(idDeficiencia = 0)),
            listOf(body, body), listOf(body.copy(descripcion = null)), listOf(body.copy(severidad = "OTRA")))) {
            assertTrue(DeficienciaRepository(api(get = { Response.success(list) })).listar(81).isFailure)
        }
    }

    @Test fun listaErroresDeServidorNoSonListaVacia() = runBlocking {
        for (code in listOf(401, 403, 404, 500)) assertTrue(DeficienciaRepository(api(get = {
            Response.error(code, "{}".toResponseBody()) })).listar(81).isFailure)
    }

    @Test fun registrarUsaIdOrdenYRespuestaDelServidor() = runBlocking {
        val repo = DeficienciaRepository(api(post = { id, datos ->
            assertEquals(81L, id); assertEquals(request, datos); Response.success(201, body)
        }))
        assertEquals(body, repo.registrar(81, request).getOrThrow())
    }

    @Test fun noEnviaFormularioInvalido() = runBlocking {
        var llamadas = 0
        val repo = DeficienciaRepository(api(post = { _, _ -> llamadas++; Response.success(201, body) }))
        assertTrue(repo.registrar(0, request).isFailure)
        assertTrue(repo.registrar(81, request.copy(descripcion = "")).isFailure)
        assertEquals(0, llamadas)
    }

    @Test fun respuestaIncoherenteEsIncierta() = runBlocking {
        for (respuesta in listOf(body.copy(idOrden = 12), body.copy(descripcion = "Otro"), body.copy(idDeficiencia = 0))) {
            assertTrue(DeficienciaRepository(api(post = { _, _ -> Response.success(201, respuesta) }))
                .registrar(81, request).exceptionOrNull() is RegistroDeficienciaInciertoException)
        }
    }

    @Test fun distingueErrorDeNegocioDeResultadoIncierto() = runBlocking {
        for (code in listOf(400, 401, 403, 409)) {
            val error = DeficienciaRepository(api(post = { _, _ -> Response.error(code,
                """{"error":"La orden no está en proceso"}""".toResponseBody()) })).registrar(81, request).exceptionOrNull()
            assertNotNull(error)
            assertFalse(error is RegistroDeficienciaInciertoException)
        }
        for (code in listOf(408, 500, 503)) assertTrue(DeficienciaRepository(api(post = { _, _ ->
            Response.error(code, "{}".toResponseBody()) })).registrar(81, request).exceptionOrNull() is RegistroDeficienciaInciertoException)
        assertTrue(DeficienciaRepository(api(post = { _, _ -> throw java.io.IOException() }))
            .registrar(81, request).exceptionOrNull() is RegistroDeficienciaInciertoException)
    }

    @Test fun propagaCancelacionSinReintentar() = runBlocking {
        val repo = DeficienciaRepository(api(get = { throw CancellationException() }, post = { _, _ -> throw CancellationException() }))
        try { repo.listar(81); fail("Debe cancelar") } catch (_: CancellationException) { }
        try { repo.registrar(81, request); fail("Debe cancelar") } catch (_: CancellationException) { }
    }

    @Test fun contratoHttpRealGetYPost() = runBlocking {
        val server = java.net.ServerSocket(0)
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val captured = executor.submit<List<String>> {
                (0..1).map { n -> server.accept().use { socket ->
                    socket.soTimeout = 5000
                    val reader = socket.getInputStream().bufferedReader()
                    val first = reader.readLine()
                    var length = 0
                    while (true) {
                        val line = reader.readLine()
                        if (line.isNullOrEmpty()) break
                        if (line.startsWith("Content-Length:", true)) length = line.substringAfter(':').trim().toInt()
                    }
                    if (n == 1) {
                        val chars = CharArray(length); var offset = 0
                        while (offset < length) { val read = reader.read(chars, offset, length - offset); check(read > 0); offset += read }
                        assertEquals(request, Gson().fromJson(String(chars), DeficienciaRequest::class.java))
                    }
                    val bytes = (if (n == 0) Gson().toJson(listOf(body)) else Gson().toJson(body)).toByteArray()
                    socket.getOutputStream().apply {
                        write(("HTTP/1.1 ${if (n == 0) "200 OK" else "201 Created"}\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray())
                        write(bytes); flush()
                    }; first
                } }
            }
            val httpApi = retrofit2.Retrofit.Builder().baseUrl("http://127.0.0.1:${server.localPort}/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(DeficienciaApi::class.java)
            val repo = DeficienciaRepository(httpApi)
            assertEquals(listOf(body), repo.listar(81).getOrThrow())
            assertEquals(body, repo.registrar(81, request).getOrThrow())
            assertEquals(listOf("GET /work-orders/81/deficiencies HTTP/1.1", "POST /work-orders/81/deficiencies HTTP/1.1"),
                captured.get(5, java.util.concurrent.TimeUnit.SECONDS))
        } finally { server.close(); executor.shutdownNow() }
    }
}
