package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.data.remote.PagoApi
import com.tecsup.autosmart.data.remote.VehiculoApi
import com.tecsup.autosmart.data.repository.PagoRepository
import com.tecsup.autosmart.data.repository.PagoSinConfirmarException
import com.tecsup.autosmart.model.CostoOrdenDto
import com.tecsup.autosmart.model.HistorialVehiculoDto
import com.tecsup.autosmart.model.MetodoPagoDto
import com.tecsup.autosmart.model.PagoDto
import com.tecsup.autosmart.model.PagoRequest
import com.tecsup.autosmart.model.VehiculoRequest
import com.tecsup.autosmart.model.VehiculoResponse
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import java.io.IOException

/** Sprint 4 · T61.2, T62.2 y T63.2 (contratos de feature/backend-payments). */
class PagoTest {
    private val gson = Gson()
    private val costo = gson.fromJson(
        """{"idOrden":7,"idCita":21,"ticket":"AS-0021","estadoCita":"FINALIZADA",
            "servicio":{"nombre":"Cambio de aceite","precio":120.00},
            "adicionales":[{"tipo":"REPUESTO","descripcion":"Filtro","cantidad":1,"precioUnitario":35.50,"subtotal":35.50}],
            "totalAdicionales":35.50,"repuestosUtilizados":[{"material":"Aceite 5W30","cantidad":4,"unidad":"L"}],
            "total":155.50,"moneda":"PEN","puedePagar":true,"pago":null}""",
        CostoOrdenDto::class.java
    )

    private fun pago(estado: String, motivo: String? = null) = PagoDto(
        idPago = 3, codigo = "PAG-2026-00003", idOrden = 7, ticket = "AS-0021", metodo = "TARJETA",
        estado = estado, monto = costo.total, fecha = "2026-11-21T10:00:00", motivoRechazo = motivo,
        comprobanteUrl = null, registradoPor = null
    )

    @Test
    fun usaLosEndpointsDelBackend() {
        fun ruta(nombre: String) = PagoApi::class.java.methods.single { it.name == nombre }
        assertEquals("work-orders/{id}/cost", ruta("costo").getAnnotation(GET::class.java)?.value)
        assertEquals("payment-methods", ruta("metodos").getAnnotation(GET::class.java)?.value)
        assertEquals("payments", ruta("pagar").getAnnotation(POST::class.java)?.value)
    }

    @Test
    fun elResumenConservaServicioAdicionalesYTotal() = runBlocking {
        val r = repositorio(costo = { Response.success(costo) }).costo(7).getOrThrow()
        assertEquals("155.50", r.total?.toPlainString())
        assertEquals(1, r.adicionales?.size)
        assertEquals("Aceite 5W30", r.repuestosUtilizados?.first()?.material)
        assertTrue(r.puedePagar)
    }

    @Test
    fun unCostoDeOtraOrdenUnIdInvalidoOUn403NoSeMuestran() = runBlocking {
        assertTrue(repositorio(costo = { Response.success(costo.copy(idOrden = 8)) }).costo(7).isFailure)
        assertTrue(repositorio(costo = { Response.error(403, "{\"error\":\"Esta orden no pertenece a tu cuenta\"}".toResponseBody()) }).costo(7).isFailure)
        assertTrue(repositorio(costo = { error("No debe consultar") }).costo(0).isFailure)
    }

    @Test
    fun soloMuestraMetodosValidosSinRepetir() = runBlocking {
        val lista = listOf(MetodoPagoDto("TARJETA", "Tarjeta", true), MetodoPagoDto("TARJETA", "Tarjeta", true),
            MetodoPagoDto("EFECTIVO", "Efectivo", false), MetodoPagoDto("", null, true))
        val r = repositorio(metodos = { Response.success(lista) }).metodos().getOrThrow()
        assertEquals(listOf("TARJETA", "EFECTIVO"), r.map { it.codigo })
        assertTrue(repositorio(metodos = { Response.success(emptyList()) }).metodos().isFailure)
    }

    @Test
    fun enviaElPagoYDevuelveAprobadoORechazadoConMotivo() = runBlocking {
        var enviado: PagoRequest? = null
        val ok = repositorio(pagar = { enviado = it; Response.success(pago("APROBADO")) })
            .pagar(7, "TARJETA", " pm_card_visa ").getOrThrow()
        assertEquals(PagoRequest(7, "TARJETA", "pm_card_visa"), enviado)
        assertEquals("APROBADO", ok.estado)

        val rechazo = repositorio(pagar = { Response.success(pago("RECHAZADO", "Fondos insuficientes")) })
            .pagar(7, "TARJETA", "pm_card_chargeDeclinedInsufficientFunds").getOrThrow()
        assertEquals("Fondos insuficientes", rechazo.motivoRechazo)
    }

    @Test
    fun unErrorDeRedAlPagarPideComprobarAntesDeReintentar() = runBlocking {
        val r = repositorio(pagar = { throw IOException("timeout") }).pagar(7, "YAPE_PLIN", "123456")
        assertTrue(r.exceptionOrNull() is PagoSinConfirmarException)
        val yaPagada = repositorio(pagar = { Response.error(409, "{\"error\":\"La orden ya fue pagada\"}".toResponseBody()) })
            .pagar(7, "TARJETA", "pm_card_visa")
        assertEquals("La orden ya fue pagada", yaPagada.exceptionOrNull()?.message)
    }

    @Test
    fun ubicaLaOrdenDeUnaCitaPorLaPlacaDelVehiculo() = runBlocking {
        val historial = gson.fromJson(
            """{"idVehiculo":5,"mantenimientos":[{"idOrden":7,"idCita":21,"estado":"FINALIZADA"},{"idOrden":4,"idCita":9}]}""",
            HistorialVehiculoDto::class.java
        )
        val repo = repositorio(vehiculos = listOf(VehiculoResponse(5, "ABC-123", "Toyota", "Yaris", 2020, 1000, 1, "Yony")),
            historial = { id -> assertEquals(5L, id); Response.success(historial) })
        assertEquals(7L, repo.ordenDeCita(21, "abc-123").getOrThrow())
        assertTrue(repo.ordenDeCita(99, "ABC-123").isFailure)
        assertTrue(repo.ordenDeCita(21, "XYZ-999").isFailure)
    }

    private fun repositorio(
        costo: suspend (Long) -> Response<CostoOrdenDto> = { error("No se esperaba consultar el costo") },
        metodos: suspend () -> Response<List<MetodoPagoDto>> = { error("No se esperaba consultar métodos") },
        pagar: suspend (PagoRequest) -> Response<PagoDto> = { error("No se esperaba pagar") },
        vehiculos: List<VehiculoResponse> = emptyList(),
        historial: suspend (Long) -> Response<HistorialVehiculoDto> = { error("No se esperaba el historial") }
    ) = PagoRepository(
        api = object : PagoApi {
            override suspend fun costo(idOrden: Long) = costo(idOrden)
            override suspend fun metodos() = metodos()
            override suspend fun pagar(request: PagoRequest) = pagar(request)
            override suspend fun comprobante(idPago: Long): Response<okhttp3.ResponseBody> = error("no usado")
            override suspend fun calificar(idOrden: Long, request: com.tecsup.autosmart.model.CalificacionRequest):
                Response<com.tecsup.autosmart.model.CalificacionDto> = error("no usado")
        },
        vehiculos = object : VehiculoApi {
            override suspend fun historial(id: Long) = historial(id)
            override suspend fun eliminarVehiculo(id: Long): Response<Unit> = error("no usado")
            override suspend fun actualizarVehiculo(id: Long, request: VehiculoRequest): Response<VehiculoResponse> = error("no usado")
            override suspend fun listarVehiculos(): Response<List<VehiculoResponse>> = Response.success(vehiculos)
            override suspend fun registrarVehiculo(request: VehiculoRequest): Response<VehiculoResponse> = error("no usado")
        }
    )
}
