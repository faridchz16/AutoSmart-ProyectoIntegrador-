package com.tecsup.autosmart.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.ia.AsistenteGemini
import com.tecsup.autosmart.data.ia.ConversacionIA
import com.tecsup.autosmart.data.ia.construirInstrucciones
import com.tecsup.autosmart.data.ia.separarAccion
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.repository.ChatbotRepository
import com.tecsup.autosmart.data.repository.PerfilRepository
import com.tecsup.autosmart.data.repository.ServicioRepository
import com.tecsup.autosmart.data.repository.VehiculoRepository
import com.tecsup.autosmart.model.ChatbotResponse
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

/** Tarjetas que el chat muestra según los datos que devuelve el backend. */
sealed interface TarjetaChat {
    /** T76.2: estado del vehículo con su avance. */
    data class Estado(val ticket: String?, val placa: String?, val estado: String?, val avance: Int?) : TarjetaChat
    /** T77.1: cotización referencial con rango de precio. */
    data class Cotizacion(val servicio: String?, val desde: Double?, val hasta: Double?) : TarjetaChat
    /** T78.2: resumen antes de confirmar la cita. */
    data class Resumen(val servicio: String?, val placa: String?, val fecha: String?, val hora: String?, val mecanico: String?) : TarjetaChat
    /** T78.2: cita creada desde el chat. */
    data class CitaCreada(val ticket: String?, val estado: String?) : TarjetaChat
    /** T79.2: solicitud registrada para que un asesor contacte al cliente. */
    data class Derivacion(val numero: Long?) : TarjetaChat
}

data class MensajeChat(
    val id: Long,
    val deUsuario: Boolean,
    val texto: String,
    val imagen: Bitmap? = null,
    val reservar: Boolean = false,
    val error: Boolean = false,
    val opciones: List<String> = emptyList(),
    val tarjeta: TarjetaChat? = null,
    val conIA: Boolean = false
)

const val OPCION_ASESOR = "Hablar con un asesor"
const val OPCION_MIS_CITAS = "Ver mis citas"

/** Opciones rápidas iniciales (T75.2): coinciden con las intenciones del chatbot del backend. */
val OPCIONES_RAPIDAS = listOf(
    "Ver servicios", "Horario de atención", "¿Cómo va mi vehículo?", "Cotizar un servicio",
    "Agendar una cita", "Preguntas frecuentes", OPCION_ASESOR
)

data class AsistenteUiState(
    val mensajes: List<MensajeChat> = listOf(
        MensajeChat(
            0, false,
            "¡Hola! Soy ARI, el asistente de AutoSmart. Te ayudo con servicios, precios, horarios, el estado de tu vehículo " +
                "y a agendar tu cita. También puedes enviarme una foto de tu auto y te digo qué veo.",
            opciones = OPCIONES_RAPIDAS
        )
    ),
    val texto: String = "",
    val imagen: Bitmap? = null,
    val escribiendo: Boolean = false
) {
    /** Opciones del último mensaje del asistente (o las iniciales). */
    val opcionesActuales: List<String>
        get() = mensajes.lastOrNull { !it.deUsuario && it.opciones.isNotEmpty() }?.opciones ?: OPCIONES_RAPIDAS
}

/** Crea la IA de fotos con los datos reales del cliente (servicios, vehículos y citas). */
suspend fun crearAsistenteReal(): ConversacionIA = coroutineScope {
    val perfil = async { PerfilRepository().obtenerPerfil().getOrNull() }
    val servicios = async { ServicioRepository().listar("").getOrNull().orEmpty() }
    val vehiculos = async { VehiculoRepository().listar().getOrNull().orEmpty() }
    val citas = async { runCatching { RetrofitClient.citaApi.listarMisCitas().body() }.getOrNull().orEmpty() }
    AsistenteGemini(construirInstrucciones(perfil.await()?.nombre, servicios.await(), vehiculos.await(), citas.await()))
}

/** Convierte los datos del backend en una tarjeta según la intención. */
fun tarjetaDe(intencion: String?, datos: Map<String, Any?>?): TarjetaChat? {
    if (datos.isNullOrEmpty()) return null
    fun texto(clave: String) = datos[clave]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
    fun numero(clave: String) = (datos[clave] as? Number)?.toDouble() ?: texto(clave)?.toDoubleOrNull()
    return when {
        intencion == "consultar_estado" -> TarjetaChat.Estado(texto("ticket"), texto("placa"), texto("estado"), numero("avance")?.toInt())
        intencion == "cotizar" -> TarjetaChat.Cotizacion(texto("servicio"), numero("precioDesde"), numero("precioHasta"))
        intencion == "agendar" && datos.containsKey("idCita") -> TarjetaChat.CitaCreada(texto("ticket"), texto("estado"))
        intencion == "agendar" && datos.containsKey("fecha") ->
            TarjetaChat.Resumen(texto("servicio"), texto("placa"), texto("fecha"), texto("hora")?.take(5), texto("mecanico"))
        intencion == "hablar_asesor" -> TarjetaChat.Derivacion(numero("idDerivacion")?.toLong())
        else -> null
    }
}

class AsistenteViewModel(
    private val chatbot: ChatbotRepository = ChatbotRepository(),
    private val crearIA: suspend () -> ConversacionIA = { crearAsistenteReal() }
) : ViewModel() {
    private val _estado = MutableStateFlow(AsistenteUiState())
    val estado: StateFlow<AsistenteUiState> = _estado.asStateFlow()
    private var ia: ConversacionIA? = null
    private var idConversacion: String? = null
    private var envio: Job? = null
    private var siguienteId = 1L

    fun editarTexto(texto: String) = _estado.update { it.copy(texto = texto.take(500)) }
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

    /** T79.2: botón "Hablar con un asesor". */
    fun pedirAsesor() = enviar(OPCION_ASESOR)

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
                if (mensaje.imagen != null) responderConIA(mensaje) else responderConTaller(mensaje)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MensajeChat(siguienteId++, false, mensajeDeErrorIA(e), error = true)
            }
            _estado.update { it.copy(mensajes = it.mensajes + respuesta, escribiendo = false) }
        }
    }

    /** Texto: lo responde el chatbot del taller; si no entiende, intenta con la IA. */
    private suspend fun responderConTaller(mensaje: MensajeChat): MensajeChat {
        val r: ChatbotResponse = chatbot.enviar(mensaje.texto, idConversacion).getOrThrow()
        idConversacion = r.idConversacion ?: idConversacion
        val delTaller = MensajeChat(
            siguienteId++, false, r.respuesta.orEmpty(),
            opciones = r.opciones.orEmpty().filterNot { it.equals("Iniciar sesión", ignoreCase = true) },
            tarjeta = tarjetaDe(r.intencion, r.datos)
        )
        if (r.intencion != "no_entendido") return delTaller
        // Pregunta libre que el chatbot no reconoce: la IA responde con los datos del taller
        return runCatching { responderConIA(mensaje) }.getOrNull()?.takeIf { !it.error } ?: delTaller
    }

    /** Fotos (y preguntas libres): IA de Gemini con el contexto real del cliente. */
    private suspend fun responderConIA(mensaje: MensajeChat): MensajeChat {
        val conversacion = ia ?: crearIA().also { ia = it }
        val (texto, reservar) = separarAccion(conversacion.enviar(mensaje.texto, mensaje.imagen))
        return MensajeChat(siguienteId++, false, texto, reservar = reservar, conIA = true,
            opciones = if (reservar) listOf("Agendar una cita") else emptyList())
    }

    fun limpiar() {
        envio?.cancel()
        ia = null
        idConversacion = null
        siguienteId = 1L
        _estado.value = AsistenteUiState()
    }
}

/** Mensaje entendible para el cliente según el error. */
fun mensajeDeErrorIA(e: Throwable): String {
    val tipo = e::class.simpleName.orEmpty()
    val detalle = e.message.orEmpty()
    return when {
        e is IOException || detalle.contains("Unable to resolve host", true) || detalle.contains("conectar", true) ->
            "No tengo conexión en este momento. Revisa tu internet e inténtalo de nuevo."
        tipo.contains("Quota", true) || detalle.contains("429") || detalle.contains("quota", true) ->
            "Recibí muchas consultas seguidas. Espera un minuto e inténtalo otra vez."
        tipo.contains("Blocked", true) || tipo.contains("Safety", true) ->
            "No puedo responder a eso. Prueba preguntándome de otra forma."
        detalle.contains("App Check", true) || detalle.contains("403") || detalle.contains("API has not been used", true) ||
            tipo.contains("ServiceDisabled", true) ->
            "El análisis de fotos con IA todavía no está activado en Firebase (Firebase AI Logic / App Check)."
        else -> detalle.takeIf { it.isNotBlank() && it.length < 160 } ?: "No pude responder en este momento. Inténtalo de nuevo."
    }
}
