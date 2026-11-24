package com.tecsup.autosmart

import com.tecsup.autosmart.data.remote.ChatbotApi
import com.tecsup.autosmart.data.remote.PagoApi
import com.tecsup.autosmart.data.remote.VehiculoApi
import com.tecsup.autosmart.data.repository.ChatbotRepository
import com.tecsup.autosmart.data.repository.PagoRepository
import com.tecsup.autosmart.data.repository.YaCalificadoException
import com.tecsup.autosmart.data.repository.nombreDeArchivo
import com.tecsup.autosmart.model.CalificacionDto
import com.tecsup.autosmart.model.CalificacionRequest
import com.tecsup.autosmart.model.ChatbotRequest
import com.tecsup.autosmart.model.ChatbotResponse
import com.tecsup.autosmart.model.CostoOrdenDto
import com.tecsup.autosmart.model.HistorialVehiculoDto
import com.tecsup.autosmart.model.MecanicoDisponible
import com.tecsup.autosmart.model.MetodoPagoDto
import com.tecsup.autosmart.model.PagoDto
import com.tecsup.autosmart.model.PagoRequest
import com.tecsup.autosmart.model.VehiculoRequest
import com.tecsup.autosmart.model.VehiculoResponse
import com.tecsup.autosmart.model.ordenarPorCalificacion
import com.tecsup.autosmart.viewmodel.TarjetaChat
import com.tecsup.autosmart.viewmodel.tarjetaDe
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

/** TEC4.3: pruebas de comprobante (T64.2), calificación (T65.2), estrellas (T66.1) y chatbot (T75.2–T79.2). */
class Sprint4Test {

    // ---------- T64.2 Comprobante ----------
    @Test
    fun comprobanteUsaElEndpointYTomaElNombreDelPdf() = runBlocking {
        assertEquals("payments/{id}/receipt",
            PagoApi::class.java.methods.single { it.name == "comprobante" }.getAnnotation(GET::class.java)?.value)
        assertEquals("comprobante-PAG-2026-00003.pdf",
            nombreDeArchivo("attachment; filename=\"comprobante-PAG-2026-00003.pdf\"", 3))
        assertEquals("comprobante-3.pdf", nombreDeArchivo(null, 3))

        val pdf = "%PDF-1.7 contenido".toByteArray()
        val ok = pagos(comprobante = {
            Response.success(pdf.toResponseBody("application/pdf".toMediaType()),
                Headers.headersOf("Content-Disposition", "attachment; filename=\"comprobante-PAG-2026-00003.pdf\""))
        }).comprobante(3).getOrThrow()
        assertEquals("comprobante-PAG-2026-00003.pdf", ok.nombre)
        assertTrue(pagos(comprobante = { Response.success("<html>".toResponseBody()) }).comprobante(3).isFailure)
        assertTrue(pagos().comprobante(0).isFailure)
    }

    // ---------- T65.2 Calificación ----------
    @Test
    fun calificaUnaVezConEstrellasYComentarioLimpio() = runBlocking {
        assertEquals("work-orders/{id}/rating",
            PagoApi::class.java.methods.single { it.name == "calificar" }.getAnnotation(POST::class.java)?.value)
        var enviado: CalificacionRequest? = null
        val repo = pagos(calificar = { id, req ->
            enviado = req
            Response.success(CalificacionDto(1, id, "Juan Rodríguez Quintero", req.estrellas, req.comentario, "2026-11-23"))
        })
        assertEquals(5, repo.calificar(7, 5, "  Muy buena atención ").getOrThrow().estrellas)
        assertEquals(CalificacionRequest(5, "Muy buena atención"), enviado)
        assertNull(repo.calificar(7, 4, "   ").getOrThrow().comentario)

        val yaHecha = pagos(calificar = { _, _ -> Response.error(409, "{\"error\":\"Este servicio ya fue calificado\"}".toResponseBody()) })
            .calificar(7, 5, "")
        assertTrue(yaHecha.exceptionOrNull() is YaCalificadoException)
        assertTrue(pagos().calificar(7, 0, "").isFailure)
        assertTrue(pagos().calificar(7, 6, "").isFailure)
        assertTrue(pagos().calificar(7, 3, "x".repeat(501)).isFailure)
    }

    // ---------- T66.1 Estrellas en la lista de mecánicos ----------
    @Test
    fun losMecanicosSeOrdenanPorCalificacionYLosNuevosVanAlFinal() {
        val lista = listOf(
            MecanicoDisponible(1, "Ana", 3, emptyList(), null, 0),
            MecanicoDisponible(2, "Beto", 5, emptyList(), 4.2, 10),
            MecanicoDisponible(3, "Carla", 8, emptyList(), 4.8, 3),
            MecanicoDisponible(4, "Dani", 2, emptyList(), 4.8, 9)
        )
        assertEquals(listOf("Dani", "Carla", "Beto", "Ana"), lista.ordenarPorCalificacion().map { it.nombre })
    }

    // ---------- T75.2 a T79.2 Chatbot ----------
    @Test
    fun elChatUsaElEndpointYMantieneLaConversacion() = runBlocking {
        assertEquals("chatbot/messages",
            ChatbotApi::class.java.methods.single { it.name == "enviar" }.getAnnotation(POST::class.java)?.value)
        var enviado: ChatbotRequest? = null
        val repo = ChatbotRepository(object : ChatbotApi {
            override suspend fun enviar(request: ChatbotRequest): Response<ChatbotResponse> {
                enviado = request
                return Response.success(ChatbotResponse("abc", "horario", "Atendemos de 8 a 18", listOf("Ver servicios"), null))
            }
        })
        val r = repo.enviar("  Horario de atención ", "abc").getOrThrow()
        assertEquals(ChatbotRequest("Horario de atención", "abc"), enviado)
        assertEquals("abc", r.idConversacion)
        assertTrue(repo.enviar("   ", null).isFailure)
    }

    @Test
    fun cadaIntencionMuestraSuTarjeta() {
        // Gson entrega los números del Map como Double
        assertEquals(TarjetaChat.Estado("AS-2026-00001", "DEF-456", "EN_PROCESO", 60),
            tarjetaDe("consultar_estado", mapOf("ticket" to "AS-2026-00001", "placa" to "DEF-456", "estado" to "EN_PROCESO", "avance" to 60.0)))
        assertEquals(TarjetaChat.Cotizacion("alineamiento", 45.0, 55.0),
            tarjetaDe("cotizar", mapOf("servicio" to "alineamiento", "precioDesde" to 45.0, "precioHasta" to 55.0, "referencial" to true)))
        assertEquals(TarjetaChat.Resumen("alineamiento", "DEF-456", "2026-11-25", "09:00", "Juan Rodríguez Quintero"),
            tarjetaDe("agendar", mapOf("servicio" to "alineamiento", "placa" to "DEF-456", "fecha" to "2026-11-25",
                "hora" to "09:00:00", "mecanico" to "Juan Rodríguez Quintero")))
        assertEquals(TarjetaChat.CitaCreada("AS-2026-00002", "PENDIENTE"),
            tarjetaDe("agendar", mapOf("idCita" to 2.0, "ticket" to "AS-2026-00002", "estado" to "PENDIENTE")))
        assertEquals(TarjetaChat.Derivacion(14), tarjetaDe("hablar_asesor", mapOf("idDerivacion" to 14.0)))
        assertNull(tarjetaDe("horario", mapOf("x" to 1)))
        assertNull(tarjetaDe("consultar_estado", null))
    }

    private fun pagos(
        comprobante: suspend (Long) -> Response<ResponseBody> = { error("No se esperaba descargar") },
        calificar: suspend (Long, CalificacionRequest) -> Response<CalificacionDto> = { _, _ -> error("No se esperaba calificar") }
    ) = PagoRepository(
        api = object : PagoApi {
            override suspend fun costo(idOrden: Long): Response<CostoOrdenDto> = error("no usado")
            override suspend fun metodos(): Response<List<MetodoPagoDto>> = error("no usado")
            override suspend fun pagar(request: PagoRequest): Response<PagoDto> = error("no usado")
            override suspend fun comprobante(idPago: Long) = comprobante(idPago)
            override suspend fun calificar(idOrden: Long, request: CalificacionRequest) = calificar(idOrden, request)
        },
        vehiculos = object : VehiculoApi {
            override suspend fun historial(id: Long): Response<HistorialVehiculoDto> = error("no usado")
            override suspend fun eliminarVehiculo(id: Long): Response<Unit> = error("no usado")
            override suspend fun actualizarVehiculo(id: Long, request: VehiculoRequest): Response<VehiculoResponse> = error("no usado")
            override suspend fun listarVehiculos(): Response<List<VehiculoResponse>> = error("no usado")
            override suspend fun registrarVehiculo(request: VehiculoRequest): Response<VehiculoResponse> = error("no usado")
        }
    )
}
