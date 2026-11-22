package com.tecsup.autosmart.dto;

public record CalificacionMecanicoResponse(
        Long idCalificacion,
        String ticket,
        String cliente,
        Integer estrellas,
        String comentario,
        String fecha
) {
}