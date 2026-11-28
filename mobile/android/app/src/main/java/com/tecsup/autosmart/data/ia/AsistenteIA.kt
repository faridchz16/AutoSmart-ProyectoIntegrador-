package com.tecsup.autosmart.data.ia

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.tecsup.autosmart.model.CitaCreadaResponse
import com.tecsup.autosmart.model.Servicio
import com.tecsup.autosmart.model.VehiculoResponse
import java.util.Locale

/** Conversación con la IA: texto y, opcionalmente, una foto del auto. */
interface ConversacionIA {
    suspend fun enviar(texto: String, imagen: Bitmap?): String
}

/** Marca que la IA agrega al final cuando conviene mostrar el botón "Reservar". */
const val MARCA_RESERVAR = "[RESERVAR]"

/**
 * Asistente AutoSmart con Gemini mediante Firebase AI Logic (Gemini Developer API, nivel gratuito).
 * La app no guarda ninguna API key: usa el proyecto de Firebase que ya tiene (google-services.json).
 */
class AsistenteGemini(instrucciones: String) : ConversacionIA {
    private val modelo = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = MODELO,
            systemInstruction = content { text(instrucciones) }
        )
    private val chat = modelo.startChat()

    override suspend fun enviar(texto: String, imagen: Bitmap?): String {
        val respuesta = chat.sendMessage(content {
            imagen?.let { image(it) }
            text(texto)
        })
        return respuesta.text?.trim().takeUnless { it.isNullOrEmpty() }
            ?: throw IllegalStateException("El asistente no devolvió una respuesta.")
    }

    companion object {
        const val MODELO = "gemini-3.5-flash"
    }
}

/** Reglas y datos reales del cliente que recibe la IA antes de conversar. */
fun construirInstrucciones(
    nombre: String?,
    servicios: List<Servicio>,
    vehiculos: List<VehiculoResponse>,
    citas: List<CitaCreadaResponse>
): String = buildString {
    appendLine("Eres ARI (Asistente de Reparación Inteligente), el asistente virtual de AutoSmart, un taller mecánico en Perú. Si te preguntan tu nombre, di que eres ARI.")
    nombre?.takeIf { it.isNotBlank() }?.let { appendLine("Hablas con el cliente $it.") }
    appendLine(
        """
        Reglas:
        - Responde siempre en español, con frases cortas (máximo 120 palabras) y un tono amable.
        - Usa solo los servicios y precios de la lista. Todo precio es REFERENCIAL: dilo siempre y aclara que el costo final se confirma en el taller.
        - No inventes horarios, promociones, mecánicos ni datos que no estén aquí.
        - Si el cliente quiere reservar o le recomiendas un servicio, explica que puede hacerlo desde la app y termina tu mensaje con $MARCA_RESERVAR.
        - Si pregunta por el estado de su vehículo o cita, usa la lista de citas. Si no hay datos, díselo.
        - Si envía una foto:
          * Si es un vehículo, describe en lenguaje sencillo lo que se ve (golpes, rayones, llantas gastadas, luces del tablero encendidas, fugas, humo) y recomienda el servicio de la lista que corresponda.
          * Aclara que es una evaluación visual referencial y que el diagnóstico real lo hace el mecánico.
          * Si ves algo peligroso (luz roja de aceite o temperatura, frenos, humo, llanta muy dañada), recomienda no manejar y acudir al taller.
          * Si la foto no es de un vehículo, dilo amablemente y pide una foto del auto.
        - Si pide hablar con una persona o no puedes ayudar, recomiéndale comunicarse con el taller.
        - No pidas ni guardes datos de tarjetas o contraseñas.
        """.trimIndent()
    )
    appendLine()
    appendLine("Servicios del taller (precio base referencial):")
    if (servicios.isEmpty()) appendLine("- (no se pudo cargar el catálogo)")
    servicios.forEach { s ->
        val precio = String.format(Locale.US, "%.2f", s.precioBase)
        val detalle = listOfNotNull(
            "S/ $precio",
            s.duracionMinutos.takeIf { it > 0 }?.let { "$it min" },
            s.descripcion?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        appendLine("- ${s.nombre}: $detalle")
    }
    appendLine()
    appendLine("Vehículos del cliente:")
    if (vehiculos.isEmpty()) appendLine("- (no tiene vehículos registrados)")
    vehiculos.forEach { v -> appendLine("- ${v.marca} ${v.modelo} ${v.anio}, placa ${v.placa}, ${v.kilometraje} km") }
    appendLine()
    appendLine("Citas del cliente:")
    if (citas.isEmpty()) appendLine("- (no tiene citas)")
    citas.forEach { c ->
        appendLine("- Ticket ${c.ticket}: ${c.servicio}, placa ${c.placa}, ${c.fecha} ${c.horaInicio.take(5)}, " +
            "mecánico ${c.mecanico}, estado ${c.estado.replace('_', ' ')}")
    }
}

/** Separa la marca [RESERVAR] del texto que ve el cliente. */
fun separarAccion(respuesta: String): Pair<String, Boolean> {
    val reservar = respuesta.contains(MARCA_RESERVAR)
    return respuesta.replace(MARCA_RESERVAR, "").trim() to reservar
}
