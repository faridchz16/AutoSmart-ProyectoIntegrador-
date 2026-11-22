package com.tecsup.autosmart.dto;

import java.math.BigDecimal;
import java.util.Map;

public record MetricasResponse(
        String mes,
        long totalCitas,
        Map<String, Long> citasPorEstado,
        BigDecimal ingresos,
        Map<String, BigDecimal> ingresosPorMetodo,
        long tiempoPromedioMinutos,
        Double calificacionPromedio,
        long totalCalificaciones
) {
}