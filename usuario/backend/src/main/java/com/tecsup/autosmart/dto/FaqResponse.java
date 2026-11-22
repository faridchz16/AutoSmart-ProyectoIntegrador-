package com.tecsup.autosmart.dto;

public record FaqResponse(
        Integer idFaq,
        String pregunta,
        String respuesta,
        String palabrasClave,
        boolean activa,
        String fechaCreacion,
        String fechaActualizacion
) {
}