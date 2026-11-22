package com.tecsup.autosmart.mapper;

import com.tecsup.autosmart.dto.CalificacionAdminResponse;
import com.tecsup.autosmart.dto.CalificacionMecanicoResponse;
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

    public CalificacionMecanicoResponse toMecanico(Calificacion calificacion) {
        return new CalificacionMecanicoResponse(
                calificacion.getIdCalificacion(),
                calificacion.getOrden().getCita().getTicket(),
                nombreCorto(calificacion.getCliente().getNombre()),
                calificacion.getEstrellas(),
                Boolean.TRUE.equals(calificacion.getOculto()) ? null : calificacion.getComentario(),
                calificacion.getFecha().toString());
    }

    public CalificacionAdminResponse toAdmin(Calificacion calificacion) {
        return new CalificacionAdminResponse(
                calificacion.getIdCalificacion(),
                calificacion.getOrden().getCita().getTicket(),
                calificacion.getCliente().getNombre(),
                calificacion.getMecanico().getNombre(),
                calificacion.getEstrellas(),
                calificacion.getComentario(),
                calificacion.getFecha().toString(),
                Boolean.TRUE.equals(calificacion.getOculto()),
                calificacion.getMotivoOcultado(),
                calificacion.getOcultadoPor(),
                calificacion.getFechaOcultado() == null ? null : calificacion.getFechaOcultado().toString());
    }

    public String nombreCorto(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "Cliente";
        }
        String[] partes = nombreCompleto.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0];
        }
        return partes[0] + " " + Character.toUpperCase(partes[1].charAt(0)) + ".";
    }
}