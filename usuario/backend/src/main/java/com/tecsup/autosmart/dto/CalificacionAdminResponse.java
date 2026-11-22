package com.tecsup.autosmart.dto;

public record CalificacionAdminResponse(
        Long idCalificacion,
        String ticket,
        String cliente,
        String mecanico,
        Integer estrellas,
        String comentario,
        String fecha,
        boolean oculto,
        String motivoOcultado,
        String ocultadoPor,
        String fechaOcultado
) {
}