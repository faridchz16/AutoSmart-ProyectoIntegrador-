package com.tecsup.autosmart.mapper;

import com.tecsup.autosmart.dto.CalificacionResponse;
import com.tecsup.autosmart.model.Calificacion;
import org.springframework.stereotype.Component;

@Component
public class CalificacionMapper {

    public CalificacionResponse toResponse(Calificacion calificacion) {
        return new CalificacionResponse(
                calificacion.getIdCalificacion(),
                calificacion.getOrden().getIdOrden(),
                calificacion.getMecanico().getNombre(),
                calificacion.getEstrellas(),
                calificacion.getComentario(),
                calificacion.getFecha().toString());
    }
}