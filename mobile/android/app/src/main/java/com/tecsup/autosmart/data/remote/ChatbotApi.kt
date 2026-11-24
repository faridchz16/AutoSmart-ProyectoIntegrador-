package com.tecsup.autosmart.data.remote

import com.tecsup.autosmart.model.ChatbotRequest
import com.tecsup.autosmart.model.ChatbotResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatbotApi {
    // T75.2: chat con el asistente del taller (servicios, horario, FAQ, estado, cotizar, agendar y asesor)
    @POST("chatbot/messages")
    suspend fun enviar(@Body request: ChatbotRequest): Response<ChatbotResponse>
}
