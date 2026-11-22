package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CalificacionRequest;
import com.tecsup.autosmart.dto.CalificacionResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.mapper.CalificacionMapper;
import com.tecsup.autosmart.model.Calificacion;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.CalificacionRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalificacionServiceTest {

    private OrdenTrabajoRepository ordenRepository;
    private CalificacionRepository calificacionRepository;
    private CalificacionService service;
    private OrdenTrabajo orden;

    @BeforeEach
    void preparar() {
        ordenRepository = mock(OrdenTrabajoRepository.class);
        calificacionRepository = mock(CalificacionRepository.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-21T15:00:00Z"), ZoneId.of("America/Lima"));
        service = new CalificacionService(ordenRepository, calificacionRepository, new CalificacionMapper(), reloj);

        Usuario cliente = new Usuario();
        cliente.setCorreo("cliente2@mail.com");
        Usuario mecanico = new Usuario();
        mecanico.setNombre("Mecanico");
        Cita cita = new Cita();
        cita.setCliente(cliente);
        cita.setMecanico(mecanico);
        cita.setEstado(EstadoCita.PAGADO);
        orden = new OrdenTrabajo();
        orden.setIdOrden(2L);
        orden.setCita(cita);

        when(ordenRepository.findById(2L)).thenReturn(Optional.of(orden));
        when(calificacionRepository.save(any(Calificacion.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void calificaUnServicioPagadoYLimpiaElComentarioVacio() {
        CalificacionResponse res = service.calificar("CLIENTE2@mail.com", 2L, new CalificacionRequest(5, "   "));

        assertEquals(5, res.estrellas());
        assertEquals("Mecanico", res.mecanico());
        assertEquals("2026-11-21T10:00", res.fecha());
        assertNull(res.comentario());
    }

    @Test
    void rechazaSiLaOrdenEsDeOtroCliente() {
        assertThrows(ForbiddenException.class,
                () -> service.calificar("otro@mail.com", 2L, new CalificacionRequest(4, null)));
    }

    @Test
    void rechazaSiElServicioNoHaTerminado() {
        orden.getCita().setEstado(EstadoCita.EN_PROCESO);

        assertThrows(ConflictException.class,
                () -> service.calificar("cliente2@mail.com", 2L, new CalificacionRequest(4, null)));
    }

    @Test
    void rechazaSiYaFueCalificado() {
        when(calificacionRepository.existsByOrden_IdOrden(2L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.calificar("cliente2@mail.com", 2L, new CalificacionRequest(4, null)));
        verify(calificacionRepository, never()).save(any());
    }

    @Test
    void lanzaNotFoundSiLaOrdenNoExiste() {
        assertThrows(NotFoundException.class,
                () -> service.calificar("cliente2@mail.com", 99L, new CalificacionRequest(4, null)));
    }
}