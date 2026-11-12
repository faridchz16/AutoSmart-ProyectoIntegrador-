package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.model.*
import com.tecsup.autosmart.data.remote.CitaApi
import com.tecsup.autosmart.data.repository.*
import com.tecsup.autosmart.viewmodel.esCancelable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.math.BigDecimal
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ReprogramarCitaTest {
    private val ahora = LocalDateTime.parse("2026-11-01T08:00:00")
    private val original = CitaCreadaResponse(12, "T-12", "CONFIRMADA", "2026-11-03", "09:00:00", "10:00:00", BigDecimal("100.00"), "Servicio", "Mecanico", "ABC-123")
    private val datos = ReprogramarRequest("2026-11-04", "11:00")
    private val actualizada = original.copy(estado = "PENDIENTE", fecha = datos.fecha, horaInicio = "11:00:00", horaFin = "12:00:00")
    private fun api(responder: suspend (Long, ReprogramarRequest) -> Response<CitaCreadaResponse>) = object : CitaApi {
        override suspend fun reprogramarCita(idCita: Long, datos: ReprogramarRequest) = responder(idCita, datos)
        override suspend fun crearCita(request: CitaRequest): Response<CitaCreadaResponse> = error("No crear")
        override suspend fun listarMisCitas(futuras: Boolean) = Response.success(listOf(original))
        override suspend fun cancelarCita(idCita: Long) = Response.success(original.copy(estado = "CANCELADA"))
        override suspend fun seguimiento(idCita: Long): Response<SeguimientoDto> = error("No seguimiento")
    }
    @Test fun elegibilidadExactaDe24HorasYEstados() {
        assertTrue(esReprogramable(original.copy(fecha="2026-11-02",horaInicio="08:00"),ahora))
        assertFalse(esReprogramable(original.copy(fecha="2026-11-02",horaInicio="07:59:59"),ahora))
        for (estado in listOf("CANCELADA","RECHAZADA","COMPLETADA")) assertFalse(esReprogramable(original.copy(estado=estado),ahora))
        assertFalse(esReprogramable(original.copy(fecha="inválida"),ahora))
        assertTrue(esCancelable(original.copy(fecha="2026-11-01",horaInicio="10:00"),ahora))
    }
    @Test fun validaFechaHoraFuturoYCambioReal() {
        assertNull(validarReprogramacion(original,datos,ahora))
        assertNotNull(validarReprogramacion(original,datos.copy(fecha="2026-02-30"),ahora))
        assertNotNull(validarReprogramacion(original,datos.copy(horaInicio="25:00"),ahora))
        assertNotNull(validarReprogramacion(original,ReprogramarRequest("2026-11-01","07:00"),ahora))
        assertNull(validarReprogramacion(original,ReprogramarRequest("2026-11-01","11:00"),ahora))
    }
    @Test fun comparaHorasConSegundosSinCambiosFicticios() {
        assertTrue(coincideHorario(original,ReprogramarRequest(original.fecha,"09:00")))
        assertNotNull(validarReprogramacion(original,ReprogramarRequest(original.fecha,"09:00"),ahora))
    }
    @Test fun jsonSoloContieneFechaYHoraInicio() {
        assertEquals(setOf("fecha","horaInicio"),Gson().toJsonTree(datos).asJsonObject.keySet())
    }
    @Test fun usaIdYActualizaConRespuestaReal() = runBlocking {
        val repo=CitaRepository(api { id,d -> assertEquals(12L,id);assertEquals(datos,d);Response.success(actualizada) })
        assertEquals(actualizada,repo.reprogramarCita(12,datos).getOrThrow())
    }
    @Test fun respuestaIncoherenteNoConfirma() = runBlocking {
        for (cita in listOf(null,actualizada.copy(idCita=99),actualizada.copy(fecha=original.fecha),
            actualizada.copy(horaInicio="15:00"),actualizada.copy(estado="CONFIRMADA"))) {
            assertTrue(CitaRepository(api { _,_->Response.success(cita) }).reprogramarCita(12,datos).exceptionOrNull() is ReprogramacionInciertaException)
        }
    }
    @Test fun rechazosDeNegocioNoSonExito() = runBlocking {
        for (code in listOf(400,401,403,404,409)) {
            val result=CitaRepository(api { _,_->Response.error(code,"".toResponseBody()) }).reprogramarCita(12,datos)
            assertTrue(result.isFailure);assertFalse(result.exceptionOrNull() is ReprogramacionInciertaException)
        }
    }
    @Test fun redYTimeoutRequierenComprobacion() = runBlocking {
        for (code in listOf(408,500,503)) assertTrue(CitaRepository(api { _,_->Response.error(code,"".toResponseBody()) }).reprogramarCita(12,datos).exceptionOrNull() is ReprogramacionInciertaException)
        val causa=java.io.IOException("Sin conexión")
        assertSame(causa,CitaRepository(api { _,_->throw causa }).reprogramarCita(12,datos).exceptionOrNull()?.cause)
    }
    @Test fun cancelacionDeCorrutinaSePropaga() = runBlocking {
        val causa=CancellationException("Salir")
        try { CitaRepository(api { _,_->throw causa }).reprogramarCita(12,datos);fail("Debe cancelar") }
        catch(e:CancellationException){assertSame(causa,e)}
    }
    @Test fun conservaLaCancelacionAnterior() = runBlocking {
        assertEquals("CANCELADA",CitaRepository(api { _,_->error("No reprogramar") }).cancelarCita(12).getOrThrow().estado)
    }
    @Test fun contratoHttpRealPutRutaYCuerpo() = runBlocking {
        val servidor=ServerSocket(0);val ejecutor=Executors.newSingleThreadExecutor()
        try {
            val recibido=ejecutor.submit<Pair<String,String>> {
                servidor.accept().use { socket ->
                    socket.soTimeout=5000
                    val lector=socket.getInputStream().bufferedReader()
                    val primera=lector.readLine();var longitud=0
                    while(true){val linea=lector.readLine();if(linea.isNullOrEmpty())break
                        if(linea.startsWith("Content-Length:",ignoreCase=true))longitud=linea.substringAfter(':').trim().toInt()}
                    val cuerpo=CharArray(longitud);var leidos=0
                    while(leidos<longitud){val n=lector.read(cuerpo,leidos,longitud-leidos);check(n>0);leidos+=n}
                    val salida=Gson().toJson(actualizada).toByteArray(Charsets.UTF_8)
                    socket.getOutputStream().apply {write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${salida.size}\r\nConnection: close\r\n\r\n".toByteArray());write(salida);flush()}
                    primera to String(cuerpo)
                }
            }
            val api=Retrofit.Builder().baseUrl("http://127.0.0.1:${servidor.localPort}/").addConverterFactory(GsonConverterFactory.create()).build().create(CitaApi::class.java)
            assertEquals(actualizada,CitaRepository(api).reprogramarCita(12,datos).getOrThrow())
            val request=recibido.get(5,TimeUnit.SECONDS)
            assertEquals("PUT /appointments/12/reschedule HTTP/1.1",request.first)
            assertEquals(datos,Gson().fromJson(request.second,ReprogramarRequest::class.java))
        } finally {servidor.close();ejecutor.shutdownNow()}
    }
}
