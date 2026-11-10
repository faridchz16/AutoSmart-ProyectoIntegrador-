package com.tecsup.autosmart

import com.tecsup.autosmart.model.VehiculoRequest
import com.tecsup.autosmart.model.VehiculoResponse
import com.tecsup.autosmart.data.remote.VehiculoApi
import com.tecsup.autosmart.data.repository.VehiculoRepository
import com.tecsup.autosmart.viewmodel.EditarVehiculoEstado
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class EditarVehiculoTest {
    private val original = VehiculoResponse(12, "ABC-123", "Toyota", "Yaris", 2020, 20000, 1, "Cliente")
    private val datos = VehiculoRequest("ABC-123", "Toyota", "Yaris", 2021, 21000)
    private val estado = EditarVehiculoEstado(original, "Toyota", "Yaris", "2021", "21000")
    private fun api(responder: suspend (Long, VehiculoRequest) -> Response<VehiculoResponse>) = object : VehiculoApi {
        override suspend fun historial(id: Long) = error("Esta prueba no consulta el historial")
        override suspend fun eliminarVehiculo(id: Long): Response<Unit> = error("No debe eliminar")
        override suspend fun listarVehiculos(): Response<List<VehiculoResponse>> = error("No debe listar")
        override suspend fun registrarVehiculo(request: VehiculoRequest): Response<VehiculoResponse> = error("No debe registrar")
        override suspend fun actualizarVehiculo(id: Long, request: VehiculoRequest) = responder(id, request)
    }

    @Test fun validaCamposObligatorios() {
        assertNull(estado.validar(2026))
        assertNotNull(estado.copy(marca = " ").validar(2026))
        assertNotNull(estado.copy(modelo = "").validar(2026))
        assertNotNull(estado.copy(original = null).validar(2026))
    }

    @Test fun validaLimitesDelAnio() {
        for (anio in listOf("", "abc", "1899", "2028", "999999999999"))
            assertNotNull(estado.copy(anio = anio).validar(2026))
        assertNull(estado.copy(anio = "1900").validar(2026))
        assertNull(estado.copy(anio = "2027").validar(2026))
    }

    @Test fun kilometrajeNoRetrocedeNiDesborda() {
        for (km in listOf("", "-1", "19999", "2.5", "2147483648"))
            assertNotNull(estado.copy(kilometraje = km).validar(2026))
        assertNull(estado.copy(kilometraje = "20000").validar(2026))
    }

    @Test fun actualizacionUtilizaIdYDatosYDevuelveServidor() = runBlocking {
        val actualizado = original.copy(anio = 2021, kilometraje = 21000)
        val repo = VehiculoRepository(api { id, request ->
            assertEquals(12L, id)
            assertEquals(datos, request)
            Response.success(actualizado)
        })
        assertEquals(actualizado, repo.actualizar(12, datos).getOrThrow())
    }

    @Test fun rechazaRespuestaVaciaODeOtroVehiculo() = runBlocking {
        for (respuesta in listOf(Response.success<VehiculoResponse>(null),
            Response.success(original.copy(idVehiculo = 99)),
            Response.success(original.copy(placa = "XYZ-999")))) {
            assertTrue(VehiculoRepository(api { _, _ -> respuesta }).actualizar(12, datos).isFailure)
        }
    }

    @Test fun erroresHttpNoConfirmanActualizacion() = runBlocking {
        for (codigo in listOf(400, 401, 403, 404, 409, 500)) {
            val repo = VehiculoRepository(api { _, _ -> Response.error(codigo, "".toResponseBody()) })
            assertTrue(repo.actualizar(12, datos).isFailure)
        }
    }

    @Test fun errorDeRedConservaCausa() = runBlocking {
        val causa = java.io.IOException("Sin conexión")
        val repo = VehiculoRepository(api { _, _ -> throw causa })
        assertSame(causa, repo.actualizar(12, datos).exceptionOrNull()?.cause)
    }

    @Test fun propagaCancelacion() = runBlocking {
        val cancelacion = CancellationException("Salir")
        val repo = VehiculoRepository(api { _, _ -> throw cancelacion })
        try {
            repo.actualizar(12, datos)
            fail("Debe propagar la cancelación")
        } catch (e: CancellationException) { assertSame(cancelacion, e) }
    }
}
