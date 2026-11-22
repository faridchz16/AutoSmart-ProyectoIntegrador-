package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.FaqRequest;
import com.tecsup.autosmart.dto.FaqResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.mapper.FaqMapper;
import com.tecsup.autosmart.model.Faq;
import com.tecsup.autosmart.repository.FaqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class FaqService {

    private final FaqRepository faqRepository;
    private final FaqMapper mapper;
    private final Clock clock;

    public FaqService(FaqRepository faqRepository, FaqMapper mapper, Clock clock) {
        this.faqRepository = faqRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FaqResponse> listar() {
        return faqRepository.findAllByOrderByIdFaqAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<Faq> activas() {
        return faqRepository.findByActivaTrueOrderByIdFaqAsc();
    }

    @Transactional
    public FaqResponse crear(FaqRequest request) {
        Faq faq = new Faq();
        aplicar(faq, request);
        faq.setActiva(true);
        faq.setFechaCreacion(ahora());
        return mapper.toResponse(faqRepository.save(faq));
    }

    @Transactional
    public FaqResponse actualizar(Integer idFaq, FaqRequest request) {
        Faq faq = buscar(idFaq);
        aplicar(faq, request);
        faq.setFechaActualizacion(ahora());
        return mapper.toResponse(faqRepository.save(faq));
    }

    @Transactional
    public FaqResponse desactivar(Integer idFaq) {
        Faq faq = buscar(idFaq);
        if (!Boolean.TRUE.equals(faq.getActiva())) {
            throw new ConflictException("La pregunta frecuente ya está desactivada");
        }
        faq.setActiva(false);
        faq.setFechaActualizacion(ahora());
        return mapper.toResponse(faqRepository.save(faq));
    }

    private Faq buscar(Integer idFaq) {
        return faqRepository.findById(idFaq)
                .orElseThrow(() -> new NotFoundException("Pregunta frecuente no encontrada"));
    }

    private void aplicar(Faq faq, FaqRequest request) {
        faq.setPregunta(request.pregunta().trim());
        faq.setRespuesta(request.respuesta().trim());
        faq.setPalabrasClave(normalizarPalabras(request.palabrasClave()));
    }

    String normalizarPalabras(String palabras) {
        if (palabras == null || palabras.isBlank()) {
            return null;
        }
        String resultado = Arrays.stream(palabras.split(","))
                .map(p -> p.trim().toLowerCase(Locale.ROOT))
                .filter(p -> !p.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
        return resultado.isEmpty() ? null : resultado;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).withNano(0);
    }
}