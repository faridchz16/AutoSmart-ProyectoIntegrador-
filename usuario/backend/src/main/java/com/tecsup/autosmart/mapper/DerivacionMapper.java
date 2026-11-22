package com.tecsup.autosmart.mapper;

import com.tecsup.autosmart.dto.DerivacionResponse;
import com.tecsup.autosmart.model.Derivacion;
import org.springframework.stereotype.Component;

@Component
public class DerivacionMapper {

    public DerivacionResponse toResponse(Derivacion d) {
        String usuario = d.getUsuario() != null ? d.getUsuario().getNombre() : "Visitante sin cuenta";
        String contacto = d.getContacto() != null
                ? d.getContacto()
                : (d.getUsuario() != null ? d.getUsuario().getCorreo() : null);
        return new DerivacionResponse(
                d.getIdDerivacion(),
                d.getIdConversacion(),
                d.getFecha().toString(),
                usuario,
                contacto,
                d.getResumen(),
                d.getEstado().name(),
                d.getNota(),
                d.getAtendidoPor(),
                d.getFechaAtencion() == null ? null : d.getFechaAtencion().toString());
    }
}