package com.tecsup.autosmart

import com.tecsup.autosmart.model.VehiculoRequest
import com.tecsup.autosmart.model.VehiculoResponse
import com.tecsup.autosmart.data.remote.VehiculoApi
import com.tecsup.autosmart.data.repository.EliminacionInciertaException
import com.tecsup.autosmart.data.repository.VehiculoRepository
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

class EliminarVehiculoTest {
    private val vehiculo = VehiculoResponse(12, "ABC-123", "Toyota", "Yaris", 2020, 20000, 1, "Cliente")
    private fun api(
        borrar: suspend (Long) -> Response<Unit> = { Response.success<Unit>(204, null) },
        listar: suspend () -> Response<List<VehiculoResponse>> = { Response.success(listOf(vehiculo)) }
    ) = object : VehiculoApi {
        override suspend fun historial(id: Long) = error("Esta prueba no consulta el historial")
        override suspend fun eliminarVehiculo(id: Long) = borrar(id)
        override suspend fun listarVehiculos() = listar()
        override suspend fun registrarVehiculo(request: VehiculoRequest): Response<VehiculoResponse> = error("No registrar")
        override suspend fun actualizarVehiculo(id: Long, request: VehiculoRequest): Response<VehiculoResponse> = error("No editar")
    }

    @Test fun solo204ConfirmaLaEliminacion() = runBlocking {
        val repo = VehiculoRepository(api(borrar = { id ->
            assertEquals(12L, id)
            Response.success<Unit>(204, null)
        }))
        assertTrue(repo.eliminar(12).isSuccess)
        for (codigo in listOf(200, 201, 202)) {
            val resultado = VehiculoRepository(api(borrar = { Response.success<Unit>(codigo, null) })).eliminar(12)
            assertTrue(resultado.exceptionOrNull() is EliminacionInciertaException)
        }
    }

    @Test fun rechazoDelServidorNoEsExito() = runBlocking {
        for (codigo in listOf(400, 401, 403, 409)) {
            val repo = VehiculoRepository(api(borrar = { Response.error(codigo, "".toResponseBody()) }))
            val resultado = repo.eliminar(12)
            assertTrue(resultado.isFailure)
            assertFalse(resultado.exceptionOrNull() is EliminacionInciertaException)
            assertFalse(resultado.exceptionOrNull()?.message.isNullOrBlank())
        }
    }

    @Test fun erroresInciertosRequierenConsulta() = runBlocking {
        for (codigo in listOf(404, 408, 500, 503)) {
            val repo = VehiculoRepository(api(borrar = { Response.error(codigo, "".toResponseBody()) }))
            assertTrue(repo.eliminar(12).exceptionOrNull() is EliminacionInciertaException)
        }
    }

    @Test fun falloDeRedConservaLaCausaYEsIncierto() = runBlocking {
        val causa = java.io.IOException("Conexión perdida")
        val resultado = VehiculoRepository(api(borrar = { throw causa })).eliminar(12)
        assertTrue(resultado.exceptionOrNull() is EliminacionInciertaException)
        assertSame(causa, resultado.exceptionOrNull()?.cause)
    }

    @Test fun cancelacionNoSeConvierteEnErrorDeNegocio() = runBlocking {
        val causa = CancellationException("Salir")
        try {
            VehiculoRepository(api(borrar = { throw causa })).eliminar(12)
            fail("Se debe propagar CancellationException")
        } catch (e: CancellationException) { assertSame(causa, e) }
    }

    @Test fun consultaDevuelveLosDatosRealesYAdmiteListaVacia() = runBlocking {
        assertEquals(listOf(vehiculo), VehiculoRepository(api()).listar().getOrThrow())
        assertEquals(emptyList<VehiculoResponse>(), VehiculoRepository(api(listar = {
            Response.success(emptyList())
        })).listar().getOrThrow())
    }

    @Test fun respuestaNulaOErrorDeConsultaNoEquivalenAListaVacia() = runBlocking {
        assertTrue(VehiculoRepository(api(listar = { Response.success(null) })).listar().isFailure)
        assertTrue(VehiculoRepository(api(listar = { Response.error(401, "".toResponseBody()) })).listar().isFailure)
        assertTrue(VehiculoRepository(api(listar = { throw java.io.IOException("Sin conexión") })).listar().isFailure)
    }

    @Test fun contratoHttpUsaDeleteYElIdentificadorCorrecto() = runBlocking {
        val servidor = ServerSocket(0)
        val ejecutor = Executors.newSingleThreadExecutor()
        try {
            val solicitud = ejecutor.submit<String> {
                servidor.accept().use { socket ->
                    socket.soTimeout = 5000
                    val lector = socket.getInputStream().bufferedReader()
                    val primera = lector.readLine()
                    while (!lector.readLine().isNullOrEmpty()) { }
                    socket.getOutputStream().write("HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n".toByteArray())
                    socket.getOutputStream().flush()
                    primera
                }
            }
            val api = Retrofit.Builder().baseUrl("http://127.0.0.1:${servidor.localPort}/")
                .addConverterFactory(GsonConverterFactory.create()).build().create(VehiculoApi::class.java)
            assertTrue(VehiculoRepository(api).eliminar(12).isSuccess)
            assertEquals("DELETE /vehicles/12 HTTP/1.1", solicitud.get(5, TimeUnit.SECONDS))
        } finally {
            servidor.close()
            ejecutor.shutdownNow()
        }
    }
}
