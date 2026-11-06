package com.tecsup.autosmart

import com.tecsup.autosmart.data.model.*
import com.tecsup.autosmart.data.network.*
import com.tecsup.autosmart.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.File

class FotoDeficienciaTest {
    private val jpeg = byteArrayOf(255.toByte(), 216.toByte(), 255.toByte(), 224.toByte(), 0, 0, 0, 0)
    private val foto = FotoCreadaResponse(7, 24, "/photos/7")
    private val deficiencia = DeficienciaResponse(24, 81, "MOTOR", "LEVE", "Fuga", "2026-11-05T10:00:00", listOf(FotoReferenciaDto(7, "/photos/7")))
    private fun defApi(body: List<DeficienciaResponse> = listOf(deficiencia)) = object : DeficienciaApi {
        override suspend fun listar(idOrden: Long) = Response.success(body)
        override suspend fun registrar(idOrden: Long, request: DeficienciaRequest): Response<DeficienciaResponse> = error("No debe registrar deficiencias")
    }
    private fun api(post: suspend (Long, Long, MultipartBody.Part) -> Response<FotoCreadaResponse> = { _, _, _ -> Response.success(201, foto) },
                    get: suspend (Long) -> Response<ResponseBody> = { Response.success(jpeg.toResponseBody()) }) = object : FotoApi {
        override suspend fun subir(idOrden: Long, idDeficiencia: Long, foto: MultipartBody.Part) = post(idOrden, idDeficiencia, foto)
        override suspend fun obtener(idFoto: Long) = get(idFoto)
    }
    private fun repo(api: FotoApi = api(), def: List<DeficienciaResponse> = listOf(deficiencia)) = FotoRepository(api, DeficienciaRepository(defApi(def)))
    private fun file() = File.createTempFile("foto-test-", ".jpg").apply { writeBytes(jpeg) }

    @Test fun reconoceSoloJpgYPngPorContenido() {
        assertEquals("image/jpeg", tipoFoto(jpeg))
        assertEquals("image/png", tipoFoto(byteArrayOf(137.toByte(),80,78,71,13,10,26,10)))
        assertNull(tipoFoto("imagen.jpg".toByteArray()))
        assertNull(tipoFoto(byteArrayOf(137.toByte(),80,78,71,0,0,0,0)))
    }

    @Test fun validaArchivoVacioTipoYLimiteReal5Mb() {
        val f = file()
        try {
            assertEquals("image/jpeg", validarArchivoFoto(f))
            java.io.RandomAccessFile(f, "rw").use { it.setLength(MAXIMO_BYTES_FOTO) }
            assertEquals("image/jpeg", validarArchivoFoto(f))
            java.io.RandomAccessFile(f, "rw").use { it.setLength(MAXIMO_BYTES_FOTO + 1) }
            assertTrue(runCatching { validarArchivoFoto(f) }.isFailure)
            f.writeBytes(byteArrayOf())
            assertTrue(runCatching { validarArchivoFoto(f) }.isFailure)
            f.writeText("GIF89a")
            assertTrue(runCatching { validarArchivoFoto(f) }.isFailure)
        } finally { f.delete() }
    }

    @Test fun consultaFotosDeLaDeficienciaCorrecta() = runBlocking {
        assertEquals(deficiencia.fotos, repo().listar(81, 24).getOrThrow())
        assertTrue(repo().listar(81, 99).isFailure)
        assertTrue(repo(def = listOf(deficiencia.copy(idOrden = 90))).listar(81, 24).isFailure)
        assertTrue(repo(def = listOf(deficiencia.copy(fotos = emptyList()))).listar(81, 24).getOrThrow().isEmpty())
    }

    @Test fun rechazaFotosInvalidasDuplicadasYUrlsAjenas() = runBlocking {
        for (fotos in listOf(null, listOf(FotoReferenciaDto(0, "/photos/0")),
            listOf(FotoReferenciaDto(7, "https://otro.example/7")), listOf(FotoReferenciaDto(7, "/photos/7"), FotoReferenciaDto(7, "/photos/7")))) {
            assertTrue(repo(def = listOf(deficiencia.copy(fotos = fotos))).listar(81, 24).isFailure)
        }
    }

    @Test fun multipartUsaCampoFotoYIdsReales() = runBlocking {
        val f = file()
        try {
            val result = repo(api(post = { orden, def, part ->
                assertEquals(81L, orden); assertEquals(24L, def)
                assertTrue(part.headers!!.get("Content-Disposition")!!.contains("name=\"foto\""))
                assertEquals("image/jpeg", part.body.contentType().toString())
                Response.success(201, foto)
            })).subir(81, 24, f)
            assertEquals(foto, result.getOrThrow())
        } finally { f.delete() }
    }

    @Test fun noEnviaArchivoInvalidoNiIdsInvalidos() = runBlocking {
        var llamadas = 0
        val r = repo(api(post = { _, _, _ -> llamadas++; Response.success(201, foto) }))
        val f = file()
        try {
            assertTrue(r.subir(0, 24, f).isFailure)
            assertTrue(r.subir(81, 0, f).isFailure)
            f.writeText("No es imagen")
            assertTrue(r.subir(81, 24, f).isFailure)
            assertEquals(0, llamadas)
        } finally { f.delete() }
    }

    @Test fun respuestasIncoherentesNoConfirmanSubida() = runBlocking {
        val f = file()
        try {
            for (body in listOf(null, foto.copy(idFoto = 0), foto.copy(idDeficiencia = 99), foto.copy(url = "/otra")))
                assertTrue(repo(api(post = { _, _, _ -> Response.success(body) })).subir(81, 24, f).exceptionOrNull() is SubidaFotoInciertaException)
        } finally { f.delete() }
    }

    @Test fun erroresDeNegocioEIncertidumbreSonDiferentes() = runBlocking {
        val f = file()
        try {
            for (code in listOf(400, 401, 403, 404, 409, 413)) {
                val error = repo(api(post = { _, _, _ -> Response.error(code, "{}".toResponseBody()) })).subir(81, 24, f).exceptionOrNull()
                assertNotNull(error);assertFalse(error is SubidaFotoInciertaException)
            }
            for (code in listOf(408, 500, 503)) assertTrue(repo(api(post = { _, _, _ -> Response.error(code, "{}".toResponseBody()) }))
                .subir(81, 24, f).exceptionOrNull() is SubidaFotoInciertaException)
            assertTrue(repo(api(post = { _, _, _ -> throw java.io.IOException() })).subir(81, 24, f).exceptionOrNull() is SubidaFotoInciertaException)
        } finally { f.delete() }
    }

    @Test fun descargaPorIdYRechazaContenidoQueNoSeaImagen() = runBlocking {
        assertArrayEquals(jpeg, repo(api(get = { id -> assertEquals(7L, id);Response.success(jpeg.toResponseBody()) })).obtener(7).getOrThrow())
        assertTrue(repo(api(get = { Response.success("<html>error</html>".toResponseBody()) })).obtener(7).isFailure)
        assertTrue(repo().obtener(0).isFailure)
        assertTrue(repo(api(get = { Response.error(403, "{}".toResponseBody()) })).obtener(7).isFailure)
    }

    @Test fun limitaDescargaConYSinContentLength() = runBlocking {
        val bytes = ByteArray(MAXIMO_BYTES_FOTO.toInt() + 1)
        assertTrue(repo(api(get = { Response.success(bytes.toResponseBody()) })).obtener(7).isFailure)
        val desconocido = object : ResponseBody() {
            override fun contentType() = null
            override fun contentLength() = -1L
            override fun source(): okio.BufferedSource = okio.Buffer().write(bytes)
        }
        assertTrue(repo(api(get = { Response.success(desconocido) })).obtener(7).isFailure)
    }

    @Test fun cancelacionSePropagaSinReintentos() = runBlocking {
        val f = file()
        try {
            try { repo(api(post = { _, _, _ -> throw CancellationException() })).subir(81, 24, f);fail("Debe cancelar") } catch (_: CancellationException) { }
            try { repo(api(get = { throw CancellationException() })).obtener(7);fail("Debe cancelar") } catch (_: CancellationException) { }
        } finally { f.delete() }
    }

    @Test fun httpRealMultipartYConsultaDeImagen() = runBlocking {
        val server = java.net.ServerSocket(0)
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        val f = file()
        try {
            val requests = executor.submit<List<String>> {
                (0..1).map { n -> server.accept().use { socket ->
                    socket.soTimeout = 5000
                    val input = socket.getInputStream().buffered()
                    fun line(): String {
                        val bytes = java.io.ByteArrayOutputStream()
                        while (true) { val b=input.read();check(b>=0);if(b==10)break;if(b!=13)bytes.write(b) }
                        return bytes.toString("UTF-8")
                    }
                    val first=line(); var length=0
                    while(true){val header=line();if(header.isEmpty())break;if(header.startsWith("Content-Length:",true))length=header.substringAfter(':').trim().toInt()}
                    val payload=ByteArray(length);var offset=0
                    while(offset<length){val count=input.read(payload,offset,length-offset);check(count>0);offset+=count}
                    if(n==0){val text=String(payload,Charsets.ISO_8859_1);assertTrue(text.contains("name=\"foto\""));assertTrue(text.contains("Content-Type: image/jpeg"))}
                    val body=if(n==0)"""{"idFoto":7,"idDeficiencia":24,"url":"/photos/7"}""".toByteArray() else jpeg
                    socket.getOutputStream().apply {
                        write(("HTTP/1.1 ${if(n==0)"201 Created" else "200 OK"}\r\nContent-Type: ${if(n==0)"application/json" else "image/jpeg"}\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray())
                        write(body);flush()
                    };first
                } }
            }
            val http=retrofit2.Retrofit.Builder().baseUrl("http://127.0.0.1:${server.localPort}/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(FotoApi::class.java)
            val r=repo(http)
            assertEquals(foto,r.subir(81,24,f).getOrThrow())
            assertArrayEquals(jpeg,r.obtener(7).getOrThrow())
            assertEquals(listOf("POST /work-orders/81/deficiencies/24/photos HTTP/1.1","GET /photos/7 HTTP/1.1"),requests.get(5,java.util.concurrent.TimeUnit.SECONDS))
        } finally { f.delete();server.close();executor.shutdownNow() }
    }
}
