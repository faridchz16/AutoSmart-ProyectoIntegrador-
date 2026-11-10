package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.model.*
import com.tecsup.autosmart.data.remote.PresupuestoApi
import com.tecsup.autosmart.data.repository.PresupuestoRepository
import com.tecsup.autosmart.data.repository.presupuestoValido
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.math.BigDecimal

class PresupuestoTest {
    private val json = """{"idPresupuesto":9,"idOrden":81,"idCita":12,"estado":"PENDIENTE","descripcion":"Reparación adicional","fechaCreacion":"2026-11-05T10:00:00","fechaDecision":null,"items":[{"tipo":"REPUESTO","descripcion":"Pastillas","cantidad":2,"precioUnitario":12.30,"subtotal":24.60},{"tipo":"MANO_OBRA","descripcion":"Instalación","cantidad":1,"precioUnitario":20.00,"subtotal":20.00}],"total":44.60,"deficiencias":[{"idDeficiencia":24,"idOrden":81,"categoria":"FRENOS","severidad":"CRITICA","descripcion":"Desgaste","fechaRegistro":"2026-11-05T09:00:00","fotos":[{"idFoto":7,"url":"/photos/7"}]}]}"""
    private val body = Gson().fromJson(json, PresupuestoResponse::class.java)
    private fun api(
        decidir: suspend (Long, DecisionPresupuestoRequest) -> Response<PresupuestoResponse> = { _, _ ->
            error("No se esperaba una decisión")
        },
        call: suspend (Long) -> Response<PresupuestoResponse> = { Response.success(body) }) = object : PresupuestoApi {
        override suspend fun obtener(idCita: Long) = call(idCita)
        override suspend fun decidir(idPresupuesto: Long, request: DecisionPresupuestoRequest) =
            decidir.invoke(idPresupuesto, request)
    }

    @Test fun consultaPorIdCitaYConservaIdsDiferentes() = runBlocking {
        val response = PresupuestoRepository(api { id -> assertEquals(12L,id);Response.success(body) }).obtener(12).getOrThrow()!!
        assertEquals(9L,response.idPresupuesto);assertEquals(81L,response.idOrden);assertEquals(12L,response.idCita)
    }

    @Test fun mapeaDecimalesSinPerderPrecisionYFotosReales() {
        assertEquals("44.60",body.total!!.toPlainString())
        assertEquals("12.30",body.items!![0].precioUnitario!!.toPlainString())
        assertEquals("REPUESTO",body.items[0].tipo)
        assertEquals(7L,body.deficiencias!!.single().fotos!!.single().idFoto)
        assertNull(body.fechaDecision)
        assertTrue(presupuestoValido(body,12))
    }

    @Test fun diferenciaAusenciaDePresupuestoDeCitaInexistente() = runBlocking {
        for (message in listOf("La cita aun no tiene presupuesto","La orden aun no tiene presupuesto")) {
            val result=PresupuestoRepository(api { Response.error(404,"""{"error":"$message"}""".toResponseBody()) }).obtener(12)
            assertTrue(result.isSuccess);assertNull(result.getOrThrow())
        }
        for(message in listOf("Cita no encontrada","Not Found")) assertTrue(PresupuestoRepository(api {
            Response.error(404,"""{"error":"$message"}""".toResponseBody()) }).obtener(12).isFailure)
    }

    @Test fun erroresDeSesionPermisosYServidorNoSonPresupuestoVacio() = runBlocking {
        for(code in listOf(401,403,409,500)) assertTrue(PresupuestoRepository(api { Response.error(code,"{}".toResponseBody()) }).obtener(12).isFailure)
        assertTrue(PresupuestoRepository(api { throw java.io.IOException() }).obtener(12).isFailure)
    }

    @Test fun noConsultaIdsInvalidosNiMuestraOtraCita() = runBlocking {
        var llamadas=0
        val repo=PresupuestoRepository(api { llamadas++;Response.success(body) })
        assertTrue(repo.obtener(0).isFailure);assertEquals(0,llamadas)
        assertTrue(repo.obtener(99).isFailure)
        assertTrue(PresupuestoRepository(api { Response.success(null) }).obtener(12).isFailure)
    }

    @Test fun estadosRealesSeConservanSinAprobarNiRechazar() {
        for(estado in listOf("PENDIENTE","APROBADO","RECHAZADO")) assertTrue(presupuestoValido(body.copy(estado=estado),12))
        assertFalse(presupuestoValido(body.copy(estado="CONFIRMADO"),12))
    }

    @Test fun noPresentaImportesIncoherentes() {
        assertFalse(presupuestoValido(body.copy(total=BigDecimal("99.00")),12))
        assertFalse(presupuestoValido(body.copy(total=BigDecimal("-1")),12))
        assertFalse(presupuestoValido(body.copy(items=body.items!!.map { it.copy(cantidad=0) }),12))
        assertFalse(presupuestoValido(body.copy(items=body.items.map { it.copy(precioUnitario=BigDecimal("-1")) }),12))
        assertFalse(presupuestoValido(body.copy(items=body.items.map { it.copy(subtotal=BigDecimal.ZERO) }),12))
        assertFalse(presupuestoValido(body.copy(items=emptyList()),12))
    }

    @Test fun camposObligatoriosNoSeSustituyenPorDatosInventados() {
        for(p in listOf(body.copy(idPresupuesto=0),body.copy(idOrden=0),body.copy(descripcion=null),
            body.copy(fechaCreacion=null),body.copy(total=null),body.copy(items=null),body.copy(deficiencias=null))) assertFalse(presupuestoValido(p,12))
        assertTrue(presupuestoValido(body.copy(deficiencias=emptyList()),12))
    }

    @Test fun fotosYDeficienciasPertenecenALaOrdenDelPresupuesto() {
        val d=body.deficiencias!!.single()
        for(deficiencias in listOf(listOf(d.copy(idOrden=99)),listOf(d,d),listOf(d.copy(fotos=null)),
            listOf(d.copy(fotos=listOf(FotoReferenciaDto(7,"https://otro.example/7"))))))
            assertFalse(presupuestoValido(body.copy(deficiencias=deficiencias),12))
    }

    @Test fun propagaCancelacion() = runBlocking {
        try { PresupuestoRepository(api { throw CancellationException() }).obtener(12);fail("Debe cancelar") }
        catch (_:CancellationException) { }
    }

    @Test fun enviaLaDecisionPorIdPresupuestoYValidaLaRespuesta() = runBlocking {
        for (decision in DecisionPresupuesto.entries) {
            val confirmado = body.copy(estado = decision.name, fechaDecision = "2026-11-05T12:00:00")
            val repo = PresupuestoRepository(api(decidir = { id, request ->
                assertEquals(9L, id)
                assertEquals("""{"decision":"${decision.name}"}""", Gson().toJson(request))
                Response.success(confirmado)
            }))
            assertEquals(confirmado, repo.decidir(body, decision).getOrThrow())
            assertTrue(repo.decidir(confirmado, decision).isFailure)
        }
    }

    @Test fun rechazaConfirmacionesIncoherentesYConflictos() = runBlocking {
        val confirmado = body.copy(estado = "APROBADO", fechaDecision = "2026-11-05T12:00:00")
        val invalidos = listOf(body, confirmado.copy(idPresupuesto = 10), confirmado.copy(idOrden = 4),
            confirmado.copy(idCita = 13), confirmado.copy(fechaDecision = null))
        for (invalido in invalidos) {
            val repo = PresupuestoRepository(api(decidir = { _, _ -> Response.success(invalido) }))
            assertTrue(repo.decidir(body, DecisionPresupuesto.APROBADO).isFailure)
        }
        val conflicto = PresupuestoRepository(api(decidir = { _, _ ->
            Response.error(409, "{}".toResponseBody())
        }))
        assertTrue(conflicto.decidir(body, DecisionPresupuesto.APROBADO).isFailure)
    }

    @Test fun contratoHttpGetDelCliente() = runBlocking {
        val server=java.net.ServerSocket(0);val executor=java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val captured=executor.submit<String>{server.accept().use{socket->
                socket.soTimeout=5000
                val reader=socket.getInputStream().bufferedReader();val first=reader.readLine()
                while(!reader.readLine().isNullOrEmpty()) { }
                val bytes=json.toByteArray(Charsets.UTF_8)
                socket.getOutputStream().apply{
                    write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray())
                    write(bytes);flush()
                };first
            }}
            val http=retrofit2.Retrofit.Builder().baseUrl("http://127.0.0.1:${server.localPort}/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build().create(PresupuestoApi::class.java)
            assertEquals(body,PresupuestoRepository(http).obtener(12).getOrThrow())
            assertEquals("GET /appointments/12/quote HTTP/1.1",captured.get(5,java.util.concurrent.TimeUnit.SECONDS))
        } finally {server.close();executor.shutdownNow()}
    }
}
