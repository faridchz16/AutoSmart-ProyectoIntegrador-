package com.tecsup.autosmart.model

/** POST /chatbot/messages (T75.1 de Farid): el backend detecta la intención y responde. */
data class ChatbotRequest(val mensaje: String, val idConversacion: String?)

data class ChatbotResponse(
    val idConversacion: String?,
    val intencion: String?,
    val respuesta: String?,
    val opciones: List<String>?,
    val datos: Map<String, Any?>?,
    val requiereLogin: Boolean = false
)
