package com.tecsup.autosmart

import com.tecsup.autosmart.data.ia.MARCA_RESERVAR
import com.tecsup.autosmart.data.ia.construirInstrucciones
import com.tecsup.autosmart.data.ia.separarAccion
import com.tecsup.autosmart.model.CitaCreadaResponse
import com.tecsup.autosmart.model.Servicio
import com.tecsup.autosmart.model.VehiculoResponse
import com.tecsup.autosmart.viewmodel.mensajeDeErrorIA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/** Asistente AutoSmart con IA: datos que recibe y respuestas que muestra. */
class AsistenteTest {
    private val instrucciones = construirInstrucciones(
        nombre = "Diego Ramirez",
        servicios = listOf(Servicio(2, "alineamiento", "Alineación de ruedas", 50.0, 60)),
        vehiculos = listOf(VehiculoResponse(5, "DEF-456", "Hyundai", "Tucson", 2021, 48000, 4, "Diego")),
        citas = listOf(CitaCreadaResponse(1, "AS-2026-00001", "EN_PROCESO", "2026-11-23", "09:00:00", "10:00:00",
            BigDecimal("50.00"), "alineamiento", "Juan Rodríguez Quintero", "DEF-456"))
    )

    @Test
    fun laIaRecibeLosDatosRealesDelCliente() {
        assertTrue(instrucciones.contains("Diego Ramirez"))
        assertTrue(instrucciones.contains("alineamiento: S/ 50.00 · 60 min"))
        assertTrue(instrucciones.contains("Hyundai Tucson 2021, placa DEF-456"))
        assertTrue(instrucciones.contains("Ticket AS-2026-00001"))
        assertTrue(instrucciones.contains("estado EN PROCESO"))
    }

    @Test
    fun lasReglasPidenPreciosReferencialesYCuidadoConLasFotos() {
        assertTrue(instrucciones.contains("REFERENCIAL"))
        assertTrue(instrucciones.contains("evaluación visual referencial"))
        assertTrue(instrucciones.contains(MARCA_RESERVAR))
        assertTrue(instrucciones.contains("No inventes"))
    }

    @Test
    fun sinDatosLoIndicaEnVezDeInventar() {
        val vacio = construirInstrucciones(null, emptyList(), emptyList(), emptyList())
        assertTrue(vacio.contains("no se pudo cargar el catálogo"))
        assertTrue(vacio.contains("no tiene vehículos registrados"))
        assertTrue(vacio.contains("no tiene citas"))
    }

    @Test
    fun laMarcaReservarSeQuitaDelTextoYActivaElBoton() {
        assertEquals("Te recomiendo un alineamiento." to true, separarAccion("Te recomiendo un alineamiento. $MARCA_RESERVAR"))
        assertEquals("Hola" to false, separarAccion("Hola"))
    }

    @Test
    fun losErroresSeExplicanAlCliente() {
        assertTrue(mensajeDeErrorIA(IOException("timeout")).contains("internet"))
        assertTrue(mensajeDeErrorIA(Exception("HTTP 429 quota exceeded")).contains("minuto"))
        assertTrue(mensajeDeErrorIA(Exception("Firebase App Check token is invalid")).contains("Firebase"))
        assertFalse(mensajeDeErrorIA(Exception("otro")).isBlank())
    }
}
