package com.tecsup.autosmart.dto;

import java.util.List;
import java.util.Map;

public record ChatbotResponse(
        String idConversacion,
        String intencion,
        String respuesta,
        List<String> opciones,
        Map<String, Object> datos,
        boolean requiereLogin
) {
}