package com.tecsup.autosmart.mapper;

import com.tecsup.autosmart.dto.FaqResponse;
import com.tecsup.autosmart.model.Faq;
import org.springframework.stereotype.Component;

@Component
public class FaqMapper {

    public FaqResponse toResponse(Faq faq) {
        return new FaqResponse(
                faq.getIdFaq(),
                faq.getPregunta(),
                faq.getRespuesta(),
                faq.getPalabrasClave(),
                Boolean.TRUE.equals(faq.getActiva()),
                faq.getFechaCreacion() != null ? faq.getFechaCreacion().toString() : null,
                faq.getFechaActualizacion() != null ? faq.getFechaActualizacion().toString() : null);
    }
}