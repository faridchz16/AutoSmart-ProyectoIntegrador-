package com.tecsup.autosmart.reporte;

import com.tecsup.autosmart.dto.MetricasResponse;
import com.tecsup.autosmart.service.MetricasService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MetricasFuente implements FuenteReporte {

    private final MetricasService metricasService;

    public MetricasFuente(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @Override
    public String tipo() {
        return "metricas";
    }

    @Override
    public Reporte generar(Map<String, String> filtros) {
        MetricasResponse m = metricasService.delMes(FiltrosReporte.mes(filtros, "mes"));

        List<List<Object>> filas = new ArrayList<>();
        filas.add(List.of("Total de citas", m.totalCitas()));
        m.citasPorEstado().forEach((estado, total) -> filas.add(List.of("Citas " + estado, total)));
        filas.add(List.of("Ingresos (S/)", m.ingresos()));
        m.ingresosPorMetodo().forEach((metodo, total) -> filas.add(List.of("Ingresos " + metodo + " (S/)", total)));
        filas.add(List.of("Tiempo promedio de servicio (min)", m.tiempoPromedioMinutos()));
        filas.add(List.of("Calificación promedio",
                m.calificacionPromedio() == null ? "Sin calificaciones" : m.calificacionPromedio()));
        filas.add(List.of("Total de calificaciones", m.totalCalificaciones()));

        return new Reporte(
                "Métricas del mes " + m.mes(),
                "reporte-metricas-" + m.mes(),
                List.of("Indicador", "Valor"),
                filas,
                List.of());
    }
}