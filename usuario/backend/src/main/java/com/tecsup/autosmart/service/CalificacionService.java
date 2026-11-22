package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CalificacionRequest;
import com.tecsup.autosmart.dto.CalificacionResponse;
import com.tecsup.autosmart.dto.MisCalificacionesResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.mapper.CalificacionMapper;
import com.tecsup.autosmart.model.Calificacion;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.repository.CalificacionRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CalificacionService {

    private final OrdenTrabajoRepository ordenRepository;
    private final CalificacionRepository calificacionRepository;
    private final CalificacionMapper mapper;
    private final Clock clock;

    public CalificacionService(OrdenTrabajoRepository ordenRepository,
                               CalificacionRepository calificacionRepository,
                               CalificacionMapper mapper,
                               Clock clock) {
        this.ordenRepository = ordenRepository;
        this.calificacionRepository = calificacionRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public CalificacionResponse calificar(String correoCliente, Long idOrden, CalificacionRequest request) {
        OrdenTrabajo orden = ordenRepository.findById(idOrden)
                .orElseThrow(() -> new NotFoundException("Orden de trabajo no encontrada"));
        Cita cita = orden.getCita();

        if (!cita.getCliente().getCorreo().equalsIgnoreCase(correoCliente)) {
            throw new ForbiddenException("Esta orden no pertenece a tu cuenta");
        }
        if (!cita.getEstado().terminada()) {
            throw new ConflictException("Solo puedes calificar un servicio finalizado");
        }
        if (cita.getMecanico() == null) {
            throw new ConflictException("La orden no tiene un mecánico asignado");
        }
        if (calificacionRepository.existsByOrden_IdOrden(idOrden)) {
            throw new ConflictException("Este servicio ya fue calificado");
        }

        Calificacion calificacion = new Calificacion();
        calificacion.setOrden(orden);
        calificacion.setMecanico(cita.getMecanico());
        calificacion.setCliente(cita.getCliente());
        calificacion.setEstrellas(request.estrellas());
        calificacion.setComentario(limpiar(request.comentario()));
        calificacion.setFecha(LocalDateTime.now(clock).withNano(0));

        return mapper.toResponse(calificacionRepository.save(calificacion));
    }

    @Transactional(readOnly = true)
    public MisCalificacionesResponse misCalificaciones(String correoMecanico) {
        List<Calificacion> calificaciones = calificacionRepository.delMecanico(correoMecanico);
        Double promedio = calificaciones.isEmpty()
                ? null
                : Math.round(calificaciones.stream().mapToInt(Calificacion::getEstrellas).average().orElse(0) * 10) / 10.0;
        return new MisCalificacionesResponse(promedio, calificaciones.size(),
                calificaciones.stream().map(mapper::toMecanico).toList());
    }

    private String limpiar(String comentario) {
        if (comentario == null || comentario.isBlank()) {
            return null;
        }
        return comentario.trim();
    }
}