package com.tecsup.autosmart.chatbot;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SaludoManejador implements ManejadorIntencion {

    @Override
    public String intencion() {
        return "saludo";
    }

    @Override
    public int prioridad() {
        return 90;
    }

    @Override
    public boolean aplica(MensajeChat mensaje) {
        return TextoChat.contieneAlguno(mensaje, "hola", "buenas", "buenos dias", "buenas tardes",
                "buenas noches", "saludos", "hey", "que tal", "menu", "ayuda", "inicio");
    }

    @Override
    public RespuestaChat responder(MensajeChat mensaje) {
        String nombre = mensaje.conSesion() && mensaje.usuario().getNombre() != null
                ? ", " + mensaje.usuario().getNombre().trim().split("\\s+")[0]
                : "";
        return RespuestaChat.de(intencion(),
                "¡Hola" + nombre + "! Soy el asistente virtual de AutoSmart. ¿En qué te puedo ayudar?",
                List.of(OpcionesChat.AGENDAR, OpcionesChat.VER_SERVICIOS, OpcionesChat.HORARIO, OpcionesChat.PREGUNTAS_FRECUENTES),
                null);
    }
}