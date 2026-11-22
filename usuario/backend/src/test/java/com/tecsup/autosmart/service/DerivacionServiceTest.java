package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.AtenderDerivacionRequest;
import com.tecsup.autosmart.dto.DerivacionResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.mapper.DerivacionMapper;
import com.tecsup.autosmart.model.Derivacion;
import com.tecsup.autosmart.model.EmisorChat;
import com.tecsup.autosmart.model.EstadoDerivacion;
import com.tecsup.autosmart.model.MensajeChatbot;
import com.tecsup.autosmart.repository.DerivacionRepository;
import com.tecsup.autosmart.repository.MensajeChatbotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DerivacionServiceTest {

    private DerivacionRepository derivacionRepository;
    private MensajeChatbotRepository mensajeRepository;
    private DerivacionService service;

    @BeforeEach
    void preparar() {
        derivacionRepository = mock(DerivacionRepository.class);
        mensajeRepository = mock(MensajeChatbotRepository.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-22T20:00:00Z"), ZoneId.of("America/Lima"));
        service = new DerivacionService(derivacionRepository, mensajeRepository, new DerivacionMapper(), reloj);
        when(derivacionRepository.save(any(Derivacion.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registraConElResumenEnOrdenCronologico() {
        LocalDateTime f = LocalDateTime.of(2026, 11, 22, 14, 0);
        when(mensajeRepository.findTop10ByIdConversacionOrderByIdMensajeDesc("c")).thenReturn(List.of(
                new MensajeChatbot("c", null, EmisorChat.BOT, "Escríbeme tu teléfono", "hablar_asesor", f),
                new MensajeChatbot("c", null, EmisorChat.USUARIO, "quiero un asesor", "hablar_asesor", f)));

        Derivacion d = service.registrar("c", null, "987654321", "987654321");

        assertEquals("USUARIO: quiero un asesor\nBOT: Escríbeme tu teléfono\nUSUARIO: 987654321", d.getResumen());
        assertEquals(EstadoDerivacion.PENDIENTE, d.getEstado());
        assertEquals(LocalDateTime.of(2026, 11, 22, 15, 0), d.getFecha());
    }

    @Test
    void atiendeLaDerivacionYNoPermiteAtenderlaDosVeces() {
        Derivacion d = new Derivacion();
        d.setIdDerivacion(5L);
        d.setIdConversacion("c");
        d.setContacto("987654321");
        d.setResumen("USUARIO: asesor");
        d.setFecha(LocalDateTime.of(2026, 11, 22, 14, 0));
        when(derivacionRepository.findById(5L)).thenReturn(Optional.of(d));

        DerivacionResponse res = service.atender("admin@mail.com", 5L, new AtenderDerivacionRequest(" Se llamó al cliente "));

        assertEquals("ATENDIDA", res.estado());
        assertEquals("Se llamó al cliente", res.nota());
        assertEquals("admin@mail.com", res.atendidoPor());
        assertEquals("Visitante sin cuenta", res.usuario());
        assertEquals("987654321", res.contacto());
        assertThrows(ConflictException.class,
                () -> service.atender("admin@mail.com", 5L, new AtenderDerivacionRequest("otra")));
    }

    @Test
    void rechazaUnEstadoInvalidoEnElFiltro() {
        assertThrows(BadRequestException.class, () -> service.listar("XYZ"));
    }
}