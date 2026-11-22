package com.tecsup.autosmart.chatbot;

import java.util.List;
import java.util.Map;

public record RespuestaChat(
        String intencion,
        String respuesta,
        List<String> opciones,
        Map<String, Object> datos,
        boolean requiereLogin
) {

    public static RespuestaChat de(String intencion, String respuesta, List<String> opciones, Map<String, Object> datos) {
        return new RespuestaChat(intencion, respuesta, opciones, datos, false);
    }

    public static RespuestaChat requiereSesion(String intencion) {
        return new RespuestaChat(intencion, "Inicia sesión para consultar tus vehículos y citas.",
                List.of("Iniciar sesión"), null, true);
    }
}