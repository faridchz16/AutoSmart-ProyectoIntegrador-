package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.FaqRequest;
import com.tecsup.autosmart.dto.FaqResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.mapper.FaqMapper;
import com.tecsup.autosmart.model.Faq;
import com.tecsup.autosmart.repository.FaqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FaqServiceTest {

    private FaqRepository repository;
    private FaqService service;

    @BeforeEach
    void preparar() {
        repository = mock(FaqRepository.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-22T19:00:00Z"), ZoneId.of("America/Lima"));
        service = new FaqService(repository, new FaqMapper(), reloj);
        when(repository.save(any(Faq.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void creaLaFaqActivaYNormalizaLasPalabrasClave() {
        FaqResponse res = service.crear(new FaqRequest("  ¿Aceptan Yape?  ", "Sí, aceptamos Yape y Plin.",
                " Yape, PLIN,, pago , yape "));

        assertEquals("¿Aceptan Yape?", res.pregunta());
        assertEquals("yape, plin, pago", res.palabrasClave());
        assertTrue(res.activa());
        assertEquals("2026-11-22T14:00", res.fechaCreacion());
    }

    @Test
    void palabrasClaveVaciasQuedanNulas() {
        FaqResponse res = service.crear(new FaqRequest("¿Hacen garantía?", "Sí, 30 días.", " , , "));

        assertNull(res.palabrasClave());
    }

    @Test
    void desactivaSinBorrarYNoPermiteDesactivarDosVeces() {
        Faq faq = new Faq();
        faq.setIdFaq(3);
        faq.setPregunta("¿Aceptan Yape?");
        faq.setRespuesta("Sí");
        faq.setActiva(true);
        when(repository.findById(3)).thenReturn(Optional.of(faq));

        FaqResponse res = service.desactivar(3);

        assertFalse(res.activa());
        assertEquals("2026-11-22T14:00", res.fechaActualizacion());
        assertThrows(ConflictException.class, () -> service.desactivar(3));
    }

    @Test
    void actualizarUnaFaqInexistenteLanzaNotFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.actualizar(99, new FaqRequest("p", "r", null)));
    }
}