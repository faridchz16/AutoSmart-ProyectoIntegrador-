package com.tecsup.autosmart.mapper;

import com.tecsup.autosmart.dto.MetodoPagoResponse;
import com.tecsup.autosmart.model.MetodoPago;
import org.springframework.stereotype.Component;

@Component
public class MetodoPagoMapper {

    public MetodoPagoResponse toResponse(MetodoPago metodo) {
        return new MetodoPagoResponse(metodo.getCodigo(), metodo.getNombre(),
                Boolean.TRUE.equals(metodo.getEnLinea()));
    }
}