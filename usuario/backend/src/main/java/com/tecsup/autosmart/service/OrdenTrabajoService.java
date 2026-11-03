package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.OrdenTrabajoResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrdenTrabajoService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final CitaRepository citaRepository;
    private final OrdenTrabajoRepository ordenRepository;

    public OrdenTrabajoService(CitaRepository citaRepository, OrdenTrabajoRepository ordenRepository) {
        this.citaRepository = citaRepository;
        this.ordenRepository = ordenRepository;
    }

    @Transactional
    public OrdenTrabajoResponse iniciarAtencion(String correoMecanico, Long idCita) {
        Cita cita = citaRepository.buscarDetalle(idCita)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));

        if (!cita.getMecanico().getCorreo().equalsIgnoreCase(correoMecanico)) {
            throw new ForbiddenException("Solo el mecanico asignado puede iniciar esta atencion");
        }
        if (cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new ConflictException("Solo se puede iniciar una cita CONFIRMADA (estado: " + cita.getEstado() + ")");
        }
        if (ordenRepository.existsByCita_IdCita(idCita)) {
            throw new ConflictException("La atencion de esta cita ya fue iniciada");
        }

        cita.setEstado(EstadoCita.EN_PROCESO);
        citaRepository.save(cita);

        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setCita(cita);
        orden.setFechaInicio(LocalDateTime.now(ZONA_LIMA).withNano(0));

        return aRespuesta(ordenRepository.save(orden));
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponse> ordenesDelMecanico(String correoMecanico) {
        return ordenRepository.buscarDelMecanico(correoMecanico,
                        List.of(EstadoCita.EN_PROCESO, EstadoCita.ESPERA_APROBACION))
                .stream()
                .map(this::aRespuesta)
                .collect(Collectors.toList());
    }

    public OrdenTrabajo ordenDelMecanico(String correoMecanico, Long idOrden) {
        OrdenTrabajo orden = ordenRepository.findById(idOrden)
                .orElseThrow(() -> new NotFoundException("Orden de trabajo no encontrada"));
        if (!orden.getCita().getMecanico().getCorreo().equalsIgnoreCase(correoMecanico)) {
            throw new ForbiddenException("Solo el mecanico asignado puede trabajar esta orden");
        }
        return orden;
    }

    public void validarEnProceso(OrdenTrabajo orden) {
        EstadoCita estado = orden.getCita().getEstado();
        if (estado != EstadoCita.EN_PROCESO) {
            throw new ConflictException("La orden no esta EN_PROCESO (estado: " + estado + ")");
        }
    }

    private OrdenTrabajoResponse aRespuesta(OrdenTrabajo o) {
        OrdenTrabajoResponse res = new OrdenTrabajoResponse();
        res.setIdOrden(o.getIdOrden());
        res.setIdCita(o.getCita().getIdCita());
        res.setTicket(o.getCita().getTicket());
        res.setEstado(o.getCita().getEstado().name());
        res.setFechaInicio(o.getFechaInicio().toString());
        res.setServicio(o.getCita().getServicio().getNombre());
        res.setPlaca(o.getCita().getVehiculo().getPlaca());
        return res;
    }
}