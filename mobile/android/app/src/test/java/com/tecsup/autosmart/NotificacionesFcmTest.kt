package com.tecsup.autosmart

import com.google.gson.Gson
import com.tecsup.autosmart.data.notifications.citaDeAviso
import com.tecsup.autosmart.data.remote.RegistroFcmRequest
import com.tecsup.autosmart.data.remote.UsuarioApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.PUT

class NotificacionesFcmTest {
    @Test
    fun soloAbreSeguimientoParaUnaOrdenFinalizadaConCitaValida() {
        assertEquals(123L, citaDeAviso("ORDEN_FINALIZADA", "123"))
        assertNull(citaDeAviso("OTRO_AVISO", "123"))
        for (id in listOf(null, "", "abc", "0", "-1", "99999999999999999999")) {
            assertNull(citaDeAviso("ORDEN_FINALIZADA", id))
        }
    }

    @Test
    fun elRegistroYLaBajaUsanElContratoAutenticado() {
        val metodos = UsuarioApi::class.java.methods
        val registrar = metodos.single { it.name == "registrarFcm" }
        val retirar = metodos.single { it.name == "retirarFcm" }
        assertEquals("users/me/fcm-token", registrar.getAnnotation(PUT::class.java)?.value)
        assertEquals("users/me/fcm-token", retirar.getAnnotation(DELETE::class.java)?.value)
        for (metodo in listOf(registrar, retirar)) {
            assertEquals("Authorization", metodo.parameterAnnotations.first().filterIsInstance<Header>().single().value)
        }
        assertEquals("{\"token\":\"token-de-prueba\"}", Gson().toJson(RegistroFcmRequest("token-de-prueba")))
    }
}
