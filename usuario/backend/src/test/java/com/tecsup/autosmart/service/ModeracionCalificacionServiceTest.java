package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CalificacionAdminResponse;
import com.tecsup.autosmart.dto.OcultarCalificacionRequest;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.mapper.CalificacionMapper;
import com.tecsup.autosmart.model.Calificacion;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.CalificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModeracionCalificacionServiceTest {

    private CalificacionRepository repository;
    private ModeracionCalificacionService service;
    private Calificacion calificacion;

    @BeforeEach
    void preparar() {
        repository = mock(CalificacionRepository.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-22T18:30:00Z"), ZoneId.of("America/Lima"));
        service = new ModeracionCalificacionService(repository, new CalificacionMapper(), reloj);

        Usuario cliente = new Usuario();
        cliente.setNombre("Cliente Prueba");
        Usuario mecanico = new Usuario();
        mecanico.setNombre("Mecanico");
        Cita cita = new Cita();
        cita.setTicket("AS-2026-00035");
        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setCita(cita);
        calificacion = new Calificacion();
        calificacion.setOrden(orden);
        calificacion.setCliente(cliente);
        calificacion.setMecanico(mecanico);
        calificacion.setEstrellas(1);
        calificacion.setComentario("Comentario ofensivo");
        calificacion.setFecha(LocalDateTime.of(2026, 11, 21, 10, 0));

        when(repository.findById(4L)).thenReturn(Optional.of(calificacion));
        when(repository.save(any(Calificacion.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void ocultaLaCalificacionYRegistraQuienYCuando() {
        CalificacionAdminResponse res = service.ocultar("admin@mail.com", 4L,
                new OcultarCalificacionRequest("  Lenguaje ofensivo  "));

        assertTrue(res.oculto());
        assertEquals("Lenguaje ofensivo", res.motivoOcultado());
        assertEquals("admin@mail.com", res.ocultadoPor());
        assertEquals("2026-11-22T13:30", res.fechaOcultado());
        assertEquals("Comentario ofensivo", res.comentario());
        assertEquals(1, res.estrellas());
    }

    @Test
    void rechazaSiYaEstabaOculta() {
        calificacion.setOculto(true);

        assertThrows(ConflictException.class,
                () -> service.ocultar("admin@mail.com", 4L, new OcultarCalificacionRequest("Spam")));
        verify(repository, never()).save(any());
    }

    @Test
    void lanzaNotFoundSiNoExiste() {
        assertThrows(NotFoundException.class,
                () -> service.ocultar("admin@mail.com", 99L, new OcultarCalificacionRequest("Spam")));
    }
}