package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.ConteoEstado;
import com.tecsup.autosmart.dto.DuracionOrden;
import com.tecsup.autosmart.dto.MetodoPagoResponse;
import com.tecsup.autosmart.dto.MetricasResponse;
import com.tecsup.autosmart.dto.MontoMetodo;
import com.tecsup.autosmart.dto.ResumenCalificaciones;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.EstadoPago;
import com.tecsup.autosmart.repository.MetricasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricasService {

    private final MetricasRepository metricasRepository;
    private final MetodoPagoService metodoPagoService;
    private final Clock clock;

    public MetricasService(MetricasRepository metricasRepository,
                           MetodoPagoService metodoPagoService,
                           Clock clock) {
        this.metricasRepository = metricasRepository;
        this.metodoPagoService = metodoPagoService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MetricasResponse delMes(YearMonth mes) {
        YearMonth periodo = mes != null ? mes : YearMonth.now(clock);
        LocalDate primerDia = periodo.atDay(1);
        LocalDate ultimoDia = periodo.atEndOfMonth();
        LocalDateTime desde = primerDia.atStartOfDay();
        LocalDateTime hasta = ultimoDia.plusDays(1).atStartOfDay();

        Map<String, Long> citasPorEstado = citasPorEstado(metricasRepository.citasPorEstado(primerDia, ultimoDia));
        long totalCitas = citasPorEstado.values().stream().mapToLong(Long::longValue).sum();

        Map<String, BigDecimal> ingresosPorMetodo = ingresosPorMetodo(
                metricasRepository.ingresosPorMetodo(EstadoPago.APROBADO, desde, hasta));
        BigDecimal ingresos = ingresosPorMetodo.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        long tiempoPromedio = tiempoPromedioMinutos(metricasRepository.ordenesTerminadas(desde, hasta));

        ResumenCalificaciones calificaciones = metricasRepository.calificaciones(desde, hasta);
        Double promedio = calificaciones == null || calificaciones.promedio() == null
                ? null
                : Math.round(calificaciones.promedio() * 10) / 10.0;
        long totalCalificaciones = calificaciones == null || calificaciones.total() == null ? 0 : calificaciones.total();

        return new MetricasResponse(periodo.toString(), totalCitas, citasPorEstado, ingresos, ingresosPorMetodo,
                tiempoPromedio, promedio, totalCalificaciones);
    }

    private Map<String, Long> citasPorEstado(List<ConteoEstado> conteos) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (EstadoCita estado : EstadoCita.values()) {
            resultado.put(estado.name(), 0L);
        }
        for (ConteoEstado conteo : conteos) {
            resultado.put(conteo.estado().name(), conteo.total());
        }
        return resultado;
    }

    private Map<String, BigDecimal> ingresosPorMetodo(List<MontoMetodo> montos) {
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        for (MetodoPagoResponse metodo : metodoPagoService.listarHabilitados()) {
            resultado.put(metodo.codigo(), BigDecimal.ZERO);
        }
        for (MontoMetodo monto : montos) {
            resultado.put(monto.metodo(), monto.total() != null ? monto.total() : BigDecimal.ZERO);
        }
        return resultado;
    }

    private long tiempoPromedioMinutos(List<DuracionOrden> ordenes) {
        return Math.round(ordenes.stream()
                .filter(o -> o.inicio() != null && o.fin() != null && !o.fin().isBefore(o.inicio()))
                .mapToLong(o -> Duration.between(o.inicio(), o.fin()).toMinutes())
                .average()
                .orElse(0));
    }
}