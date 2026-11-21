package com.tecsup.autosmart.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.ia.AsistenteGemini
import com.tecsup.autosmart.data.ia.ConversacionIA
import com.tecsup.autosmart.data.ia.construirInstrucciones
import com.tecsup.autosmart.data.ia.separarAccion
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.repository.PerfilRepository
import com.tecsup.autosmart.data.repository.ServicioRepository
import com.tecsup.autosmart.data.repository.VehiculoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class MensajeChat(
    val id: Long,
    val deUsuario: Boolean,
    val texto: String,
    val imagen: Bitmap? = null,
    val reservar: Boolean = false,
    val error: Boolean = false
)

/** Opciones rápidas del chat (T75.2). */
val OPCIONES_RAPIDAS = listOf(
    "¿Qué servicios tienen?",
    "¿Cuánto cuesta un alineamiento?",
    "¿Cómo va mi vehículo?",
    "Quiero reservar una cita",
    "¿Qué significa una luz del tablero?"
)

data class AsistenteUiState(
    val mensajes: List<MensajeChat> = listOf(
        MensajeChat(
            0, false,
            "¡Hola! Soy el Asistente AutoSmart. Pregúntame por servicios y precios, el estado de tu vehículo, " +
                "o envíame una foto de tu auto y te digo qué veo."
        )
    ),
    val texto: String = "",
    val imagen: Bitmap? = null,
    val escribiendo: Boolean = false
)

/** Crea la conversación con los datos reales del cliente (servicios, vehículos y citas). */
suspend fun crearAsistenteReal(): ConversacionIA = coroutineScope {
    val perfil = async { PerfilRepository().obtenerPerfil().getOrNull() }
    val servicios = async { ServicioRepository().listar("").getOrNull().orEmpty() }
    val vehiculos = async { VehiculoRepository().listar().getOrNull().orEmpty() }
    val citas = async {
        runCatching { RetrofitClient.citaApi.listarMisCitas().body() }.getOrNull().orEmpty()
    }
    AsistenteGemini(construirInstrucciones(perfil.await()?.nombre, servicios.await(), vehiculos.await(), citas.await()))
}

class AsistenteViewModel(
    private val crearIA: suspend () -> ConversacionIA = { crearAsistenteReal() }
) : ViewModel() {
    private val _estado = MutableStateFlow(AsistenteUiState())
    val estado: StateFlow<AsistenteUiState> = _estado.asStateFlow()
    private var ia: ConversacionIA? = null
    private var envio: Job? = null
    private var siguienteId = 1L

    fun editarTexto(texto: String) = _estado.update { it.copy(texto = texto.take(1000)) }
    fun adjuntar(imagen: Bitmap?) = _estado.update { it.copy(imagen = imagen) }

    fun enviar(texto: String = _estado.value.texto) {
        val actual = _estado.value
        val imagen = actual.imagen
        val limpio = texto.trim().ifEmpty { if (imagen != null) "¿Qué ves en esta foto de mi auto?" else "" }
        if (limpio.isEmpty() || actual.escribiendo) return
        val mensaje = MensajeChat(siguienteId++, true, limpio, imagen)
        _estado.update { it.copy(mensajes = it.mensajes + mensaje, texto = "", imagen = null) }
        responder(mensaje)
    }

    /** Reintenta el último mensaje del cliente que no tuvo respuesta. */
    fun reintentar() {
        val ultimo = _estado.value.mensajes.lastOrNull { it.deUsuario } ?: return
        if (_estado.value.escribiendo) return
        _estado.update { it.copy(mensajes = it.mensajes.filterNot { m -> m.error }) }
        responder(ultimo)
    }

    private fun responder(mensaje: MensajeChat) {
        envio = viewModelScope.launch {
            _estado.update { it.copy(escribiendo = true) }
            val respuesta = try {
                val conversacion = ia ?: crearIA().also { ia = it }
                val (texto, reservar) = separarAccion(conversacion.enviar(mensaje.texto, mensaje.imagen))
                MensajeChat(siguienteId++, false, texto, reservar = reservar)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MensajeChat(siguienteId++, false, mensajeDeErrorIA(e), error = true)
            }
            _estado.update { it.copy(mensajes = it.mensajes + respuesta, escribiendo = false) }
        }
    }

    fun limpiar() {
        envio?.cancel()
        ia = null
        siguienteId = 1L
        _estado.value = AsistenteUiState()
    }
}

/** Mensaje entendible para el cliente según el error de la IA. */
fun mensajeDeErrorIA(e: Throwable): String {
    val tipo = e::class.simpleName.orEmpty()
    val detalle = e.message.orEmpty()
    return when {
        e is IOException || detalle.contains("Unable to resolve host", true) ->
            "No tengo conexión a internet en este momento. Revisa tu conexión e inténtalo de nuevo."
        tipo.contains("Quota", true) || detalle.contains("429") || detalle.contains("quota", true) ->
            "Recibí muchas consultas seguidas. Espera un minuto e inténtalo otra vez."
        tipo.contains("Blocked", true) || tipo.contains("Safety", true) ->
            "No puedo responder a eso. Prueba preguntándome de otra forma."
        detalle.contains("App Check", true) || detalle.contains("403") || detalle.contains("API has not been used", true) ||
            tipo.contains("ServiceDisabled", true) ->
            "El asistente todavía no está activado en Firebase (Firebase AI Logic / App Check)."
        else -> "No pude responder en este momento. Inténtalo de nuevo."
    }
}
