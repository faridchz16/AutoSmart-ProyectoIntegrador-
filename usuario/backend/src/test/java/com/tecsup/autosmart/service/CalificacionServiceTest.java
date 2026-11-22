package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CalificacionRequest;
import com.tecsup.autosmart.dto.CalificacionResponse;
import com.tecsup.autosmart.dto.MisCalificacionesResponse;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void misCalificacionesOcultaElComentarioModeradoPeroCuentaLaNota() {
        Usuario cliente = new Usuario();
        cliente.setNombre("Cliente Prueba");
        Calificacion visible = calificacion(5, "Muy bueno", false, cliente);
        Calificacion oculta = calificacion(4, "Comentario ofensivo", true, cliente);
        Calificacion tercera = calificacion(5, null, false, cliente);
        when(calificacionRepository.delMecanico("mecanico@mail.com")).thenReturn(List.of(visible, oculta, tercera));

        MisCalificacionesResponse res = service.misCalificaciones("mecanico@mail.com");

        assertEquals(3, res.total());
        assertEquals(4.7, res.promedio());
        assertEquals("Cliente P.", res.calificaciones().get(0).cliente());
        assertEquals("Muy bueno", res.calificaciones().get(0).comentario());
        assertNull(res.calificaciones().get(1).comentario());
        assertEquals(4, res.calificaciones().get(1).estrellas());
    }

    @Test
    void sinCalificacionesElPromedioEsNulo() {
        when(calificacionRepository.delMecanico("nuevo@mail.com")).thenReturn(List.of());

        MisCalificacionesResponse res = service.misCalificaciones("nuevo@mail.com");

        assertEquals(0, res.total());
        assertNull(res.promedio());
        assertTrue(res.calificaciones().isEmpty());
    }

    private Calificacion calificacion(int estrellas, String comentario, boolean oculto, Usuario cliente) {
        Calificacion c = new Calificacion();
        c.setOrden(orden);
        c.setCliente(cliente);
        c.setMecanico(orden.getCita().getMecanico());
        c.setEstrellas(estrellas);
        c.setComentario(comentario);
        c.setOculto(oculto);
        c.setFecha(LocalDateTime.of(2026, 11, 20, 10, 0));
        return c;
    }
}