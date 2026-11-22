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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetricasServiceTest {

    private MetricasRepository repository;
    private MetricasService service;

    @BeforeEach
    void preparar() {
        repository = mock(MetricasRepository.class);
        MetodoPagoService metodoPagoService = mock(MetodoPagoService.class);
        when(metodoPagoService.listarHabilitados()).thenReturn(List.of(
                new MetodoPagoResponse("TARJETA", "Tarjeta", true),
                new MetodoPagoResponse("YAPE_PLIN", "Yape / Plin", true),
                new MetodoPagoResponse("EFECTIVO", "Efectivo", false)));
        Clock reloj = Clock.fixed(Instant.parse("2026-11-21T15:00:00Z"), ZoneId.of("America/Lima"));
        service = new MetricasService(repository, metodoPagoService, reloj);
    }

    @Test
    void calculaLasMetricasDelMes() {
        when(repository.citasPorEstado(any(), any())).thenReturn(List.of(
                new ConteoEstado(EstadoCita.PAGADO, 3L),
                new ConteoEstado(EstadoCita.PENDIENTE, 2L)));
        when(repository.ingresosPorMetodo(eq(EstadoPago.APROBADO), any(), any())).thenReturn(List.of(
                new MontoMetodo("TARJETA", new BigDecimal("300.00")),
                new MontoMetodo("EFECTIVO", new BigDecimal("150.00"))));
        when(repository.ordenesTerminadas(any(), any())).thenReturn(List.of(
                new DuracionOrden(LocalDateTime.of(2026, 11, 9, 8, 0), LocalDateTime.of(2026, 11, 9, 9, 0)),
                new DuracionOrden(LocalDateTime.of(2026, 11, 10, 8, 0), LocalDateTime.of(2026, 11, 10, 10, 0))));
        when(repository.calificaciones(any(), any())).thenReturn(new ResumenCalificaciones(4.666, 3L));

        MetricasResponse res = service.delMes(YearMonth.of(2026, 11));

        assertEquals("2026-11", res.mes());
        assertEquals(5, res.totalCitas());
        assertEquals(3L, res.citasPorEstado().get("PAGADO"));
        assertEquals(0L, res.citasPorEstado().get("CANCELADA"));
        assertEquals(EstadoCita.values().length, res.citasPorEstado().size());
        assertEquals(new BigDecimal("450.00"), res.ingresos());
        assertEquals(BigDecimal.ZERO, res.ingresosPorMetodo().get("YAPE_PLIN"));
        assertEquals(90, res.tiempoPromedioMinutos());
        assertEquals(4.7, res.calificacionPromedio());
        assertEquals(3, res.totalCalificaciones());
    }

    @Test
    void sinDatosDevuelveCerosYPromedioNulo() {
        when(repository.citasPorEstado(any(), any())).thenReturn(List.of());
        when(repository.ingresosPorMetodo(any(), any(), any())).thenReturn(List.of());
        when(repository.ordenesTerminadas(any(), any())).thenReturn(List.of());
        when(repository.calificaciones(any(), any())).thenReturn(new ResumenCalificaciones(null, 0L));

        MetricasResponse res = service.delMes(null);

        assertEquals("2026-11", res.mes());
        assertEquals(0, res.totalCitas());
        assertEquals(BigDecimal.ZERO, res.ingresos());
        assertEquals(0, res.tiempoPromedioMinutos());
        assertNull(res.calificacionPromedio());
        assertEquals(0, res.totalCalificaciones());
        verify(repository).citasPorEstado(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    }
}