package com.tecsup.autosmart.dto;

public record DerivacionResponse(
        Long idDerivacion,
        String idConversacion,
        String fecha,
        String usuario,
        String contacto,
        String resumen,
        String estado,
        String nota,
        String atendidoPor,
        String fechaAtencion
) {
}