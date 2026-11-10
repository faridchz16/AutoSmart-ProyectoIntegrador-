package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.data.remote.OrdenTrabajoApi
import com.tecsup.autosmart.data.repository.OrdenTrabajoRepository
import com.tecsup.autosmart.model.DetalleMantenimientoDto
import com.tecsup.autosmart.model.OrdenTrabajoDto
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import retrofit2.http.GET

class DetalleMantenimientoTest {
    private val detalle = Gson().fromJson(
        """{"idOrden":81,"idCita":12,"estado":"FINALIZADA","checklist":null,"presupuesto":null,
            "repuestosConsumidos":[],"costoRepuestos":0.00,"totalEstimado":130.50}""",
        DetalleMantenimientoDto::class.java
    )

    @Test
    fun consultaPorIdOrdenYConservaImportesYSeccionesOpcionales() = runBlocking {
        val metodo = OrdenTrabajoApi::class.java.methods.single { it.name == "detalle" }
        assertEquals("work-orders/{id}/detail", metodo.getAnnotation(GET::class.java)?.value)
        val resultado = repositorio { id ->
            assertEquals(81L, id)
            Response.success(detalle)
        }.detalle(81).getOrThrow()
        assertEquals(12L, resultado.idCita)
        assertEquals("130.50", resultado.totalEstimado?.toPlainString())
        assertNull(resultado.checklist)
        assertNull(resultado.presupuesto)
    }

    @Test
    fun unPermisoDenegadoUnaOrdenDistintaYUnIdInvalidoNoMuestranDetalle() = runBlocking {
        assertTrue(repositorio { Response.success(detalle.copy(idOrden = 82)) }.detalle(81).isFailure)
        assertTrue(repositorio { Response.error(403, "{}".toResponseBody()) }.detalle(81).isFailure)
        assertTrue(repositorio { error("No se esperaba una consulta") }.detalle(0).isFailure)
    }

    private fun repositorio(responder: suspend (Long) -> Response<DetalleMantenimientoDto>) =
        OrdenTrabajoRepository(object : OrdenTrabajoApi {
            override suspend fun listar(): Response<List<OrdenTrabajoDto>> =
                error("Esta prueba solo consulta el detalle")

            override suspend fun detalle(idOrden: Long): Response<DetalleMantenimientoDto> = responder(idOrden)
        })
}
