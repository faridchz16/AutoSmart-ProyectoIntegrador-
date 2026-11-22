package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.OrdenHistorialMecanicoResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.mapper.HistorialMecanicoMapper;
import com.tecsup.autosmart.model.Calificacion;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.repository.CalificacionRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistorialMecanicoServiceTest {

    private OrdenTrabajoRepository ordenRepository;
    private CalificacionRepository calificacionRepository;
    private HistorialMecanicoService service;

    @BeforeEach
    void preparar() {
        ordenRepository = mock(OrdenTrabajoRepository.class);
        calificacionRepository = mock(CalificacionRepository.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-22T18:00:00Z"), ZoneId.of("America/Lima"));
        service = new HistorialMecanicoService(ordenRepository, calificacionRepository, new HistorialMecanicoMapper(), reloj);
    }

    @Test
    void devuelveLasOrdenesConLasEstrellasDeLasQueFueronCalificadas() {
        OrdenTrabajo calificada = orden(2L, "AS-2026-00035");
        OrdenTrabajo sinCalificar = orden(3L, "AS-2026-00040");
        Calificacion calificacion = new Calificacion();
        calificacion.setOrden(calificada);
        calificacion.setEstrellas(5);
        when(calificacionRepository.delMecanico("mecanico@mail.com")).thenReturn(List.of(calificacion));
        when(ordenRepository.historialDelMecanico(eq("mecanico@mail.com"), anyList(), any(), any()))
                .thenReturn(List.of(calificada, sinCalificar));

        List<OrdenHistorialMecanicoResponse> res = service.historial("mecanico@mail.com", null, null);

        assertEquals(2, res.size());
        assertEquals(5, res.get(0).estrellas());
        assertEquals("AS-2026-00035", res.get(0).ticket());
        assertNull(res.get(1).estrellas());
        assertNull(res.get(1).placa());
        verify(ordenRepository).historialDelMecanico("mecanico@mail.com",
                List.of(EstadoCita.FINALIZADA, EstadoCita.PAGADO),
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.of(2026, 11, 23, 0, 0));
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() {
        assertThrows(BadRequestException.class, () -> service.historial("mecanico@mail.com",
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)));
    }

    private OrdenTrabajo orden(Long id, String ticket) {
        Cita cita = new Cita();
        cita.setTicket(ticket);
        cita.setEstado(EstadoCita.PAGADO);
        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setIdOrden(id);
        orden.setCita(cita);
        orden.setFechaInicio(LocalDateTime.of(2026, 11, 9, 8, 5));
        return orden;
    }
}