package com.tecsup.autosmart.dto;

import java.util.List;

public record MisCalificacionesResponse(
        Double promedio,
        int total,
        List<CalificacionMecanicoResponse> calificaciones
) {
}