package com.tecsup.autosmart.dto;

public record OrdenHistorialMecanicoResponse(
        Long idOrden,
        Long idCita,
        String ticket,
        String servicio,
        String placa,
        String cliente,
        String fechaInicio,
        String fechaFin,
        String estado,
        Integer estrellas
) {
}