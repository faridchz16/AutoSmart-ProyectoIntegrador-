package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.model.*
import com.tecsup.autosmart.data.remote.ChecklistApi
import com.tecsup.autosmart.data.repository.ChecklistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class ChecklistTest {
    private val items = elementosChecklist.map { ChecklistItemDto(it, "OK", null) }
    private val request = ChecklistRequest(null, items)
    private val body = ChecklistResponse(8, 81, "2026-11-02T10:00:00", null, items)
    private fun api(
        get: suspend (Long) -> Response<ChecklistResponse> = { Response.success(body) },
        put: suspend (Long, ChecklistRequest) -> Response<ChecklistResponse> = { _, _ -> Response.success(body) }
    ) = object : ChecklistApi {
        override suspend fun obtener(idOrden: Long) = get(idOrden)
        override suspend fun guardar(idOrden: Long, request: ChecklistRequest) = put(idOrden, request)
    }

    @Test fun validaCincoElementosSinDuplicados() {
        assertNull(validarChecklist(request))
        assertNotNull(validarChecklist(request.copy(items = items.dropLast(1))))
        assertNotNull(validarChecklist(request.copy(items = items.dropLast(1) + items.first())))
    }

    @Test fun noAsumeQueTodoEstaOk() {
        assertNotNull(validarChecklist(request.copy(items = elementosChecklist.map { ChecklistItemDto(it) })))
        assertNotNull(validarChecklist(request.copy(items = items.map { it.copy(estado = "OTRO") })))
    }

    @Test fun observadoRequiereDescripcionYRespetaLimites() {
        val observado = items.first().copy(estado = "OBSERVADO", observacion = " ")
        assertNotNull(validarChecklist(request.copy(items = listOf(observado) + items.drop(1))))
        assertNull(validarChecklist(request.copy(items = listOf(observado.copy(observacion = "Luz rota")) + items.drop(1))))
        assertNotNull(validarChecklist(request.copy(observacionGeneral = "a".repeat(301))))
        assertNull(validarChecklist(request.copy(observacionGeneral = "a".repeat(300))))
        assertNotNull(validarChecklist(request.copy(items = items.map { it.copy(observacion = "a".repeat(301)) })))
    }

    @Test fun distingueAusenciaDeChecklistDeOrdenInexistente() = runBlocking {
        val sinChecklist = ChecklistRepository(api(get = { Response.error(404,
            """{"error":"La orden aun no tiene checklist"}""".toResponseBody()) })).obtener(81)
        assertTrue(sinChecklist.isSuccess)
        assertNull(sinChecklist.getOrThrow())
        for (error in listOf("Orden de trabajo no encontrada", "Not Found")) {
            assertTrue(ChecklistRepository(api(get = { Response.error(404,
                """{"error":"$error"}""".toResponseBody()) })).obtener(81).isFailure)
        }
    }

    @Test fun consultaConIdOrdenYRespuestaCoherente() = runBlocking {
        val repo = ChecklistRepository(api(get = { id -> assertEquals(81L, id); Response.success(body) }))
        assertEquals(body, repo.obtener(81).getOrThrow())
        assertTrue(repo.obtener(0).isFailure)
        assertTrue(ChecklistRepository(api(get = { Response.success(body.copy(idOrden = 12)) })).obtener(81).isFailure)
        assertTrue(ChecklistRepository(api(get = { Response.success(null) })).obtener(81).isFailure)
    }

    @Test fun guardarSoloExitosoConConfirmacionDelServidor() = runBlocking {
        assertEquals(body, ChecklistRepository(api(put = { id, req ->
            assertEquals(81L, id); assertEquals(request, req); Response.success(body)
        })).guardar(81, request).getOrThrow())
        assertTrue(ChecklistRepository(api(put = { _, _ -> Response.success(body.copy(idOrden = 12)) }))
            .guardar(81, request).isFailure)
        assertTrue(ChecklistRepository(api(put = { _, _ -> Response.success(body.copy(observacionGeneral = "distinto")) }))
            .guardar(81, request).isFailure)
        assertTrue(ChecklistRepository(api(put = { _, _ -> Response.success(body.copy(items = items.drop(1))) }))
            .guardar(81, request).isFailure)
    }

    @Test fun noEnviaFormularioInvalido() = runBlocking {
        var llamadas = 0
        val repo = ChecklistRepository(api(put = { _, _ -> llamadas++; Response.success(body) }))
        assertTrue(repo.guardar(81, request.copy(items = emptyList())).isFailure)
        assertEquals(0, llamadas)
    }

    @Test fun erroresDePermisoEstadoYRedNoSeConsideranExito() = runBlocking {
        for (code in listOf(401, 403, 409, 500)) {
            val repo = ChecklistRepository(api(get = { Response.error(code, "{}".toResponseBody()) },
                put = { _, _ -> Response.error(code, "{}".toResponseBody()) }))
            assertTrue(repo.obtener(81).isFailure)
            assertTrue(repo.guardar(81, request).isFailure)
        }
        assertTrue(ChecklistRepository(api(get = { throw java.io.IOException("Sin red") })).obtener(81).isFailure)
    }

    @Test fun propagaCancelaciones() = runBlocking {
        val repo = ChecklistRepository(api(get = { throw CancellationException() }, put = { _, _ -> throw CancellationException() }))
        try { repo.obtener(81); fail("Debe cancelar") } catch (_: CancellationException) { }
        try { repo.guardar(81, request); fail("Debe cancelar") } catch (_: CancellationException) { }
    }

    @Test fun jsonNoEnviaIdentificadoresNiFechaInventados() {
        val json = Gson().toJsonTree(request).asJsonObject
        assertEquals(setOf("items"), json.keySet())
        assertEquals(5, json.getAsJsonArray("items").size())
    }

    @Test fun contratoHttpGetYPut() = runBlocking {
        val servidor = java.net.ServerSocket(0)
        val ejecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val solicitudes = ejecutor.submit<List<String>> {
                (0..1).map { numero ->
                    servidor.accept().use { socket ->
                        socket.soTimeout = 5000
                        val reader = socket.getInputStream().bufferedReader()
                        val primera = reader.readLine()
                        var length = 0
                        while (true) {
                            val header = reader.readLine()
                            if (header.isNullOrEmpty()) break
                            if (header.startsWith("Content-Length:", ignoreCase = true)) length = header.substringAfter(':').trim().toInt()
                        }
                        if (numero == 1) {
                            val chars = CharArray(length)
                            var read = 0
                            while (read < length) read += reader.read(chars, read, length - read)
                            assertEquals(request, Gson().fromJson(String(chars), ChecklistRequest::class.java))
                        }
                        val bytes = Gson().toJson(body).toByteArray(Charsets.UTF_8)
                        socket.getOutputStream().apply {
                            write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray())
                            write(bytes); flush()
                        }
                        primera
                    }
                }
            }
            val httpApi = retrofit2.Retrofit.Builder().baseUrl("http://127.0.0.1:${servidor.localPort}/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build().create(ChecklistApi::class.java)
            val repo = ChecklistRepository(httpApi)
            assertEquals(body, repo.obtener(81).getOrThrow())
            assertEquals(body, repo.guardar(81, request).getOrThrow())
            assertEquals(listOf("GET /work-orders/81/checklist HTTP/1.1", "PUT /work-orders/81/checklist HTTP/1.1"),
                solicitudes.get(5, java.util.concurrent.TimeUnit.SECONDS))
        } finally { servidor.close(); ejecutor.shutdownNow() }
    }
}
