package com.tecsup.autosmart.mapper;

import com.tecsup.autosmart.dto.OrdenHistorialMecanicoResponse;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.OrdenTrabajo;
import org.springframework.stereotype.Component;

@Component
public class HistorialMecanicoMapper {

    public OrdenHistorialMecanicoResponse toResponse(OrdenTrabajo orden, Integer estrellas) {
        Cita cita = orden.getCita();
        return new OrdenHistorialMecanicoResponse(
                orden.getIdOrden(),
                cita.getIdCita(),
                cita.getTicket(),
                cita.getServicio() != null ? cita.getServicio().getNombre() : null,
                cita.getVehiculo() != null ? cita.getVehiculo().getPlaca() : null,
                cita.getCliente() != null ? cita.getCliente().getNombre() : null,
                orden.getFechaInicio() != null ? orden.getFechaInicio().toString() : null,
                orden.getFechaFin() != null ? orden.getFechaFin().toString() : null,
                cita.getEstado().name(),
                estrellas);
    }
}