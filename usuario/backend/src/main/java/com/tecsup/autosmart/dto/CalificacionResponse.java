package com.tecsup.autosmart.dto;

public record CalificacionResponse(
        Long idCalificacion,
        Long idOrden,
        String mecanico,
        Integer estrellas,
        String comentario,
        String fecha
) {
}