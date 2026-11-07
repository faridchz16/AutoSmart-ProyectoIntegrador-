package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.FinalizacionResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.EstadoSolicitud;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.TareaOrden;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import com.tecsup.autosmart.repository.SolicitudMaterialRepository;
import com.tecsup.autosmart.repository.TareaOrdenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class FinalizacionService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final OrdenTrabajoService ordenService;
    private final OrdenTrabajoRepository ordenRepository;
    private final CitaRepository citaRepository;
    private final TareaOrdenRepository tareaRepository;
    private final SolicitudMaterialRepository solicitudRepository;

    public FinalizacionService(OrdenTrabajoService ordenService,
                               OrdenTrabajoRepository ordenRepository,
                               CitaRepository citaRepository,
                               TareaOrdenRepository tareaRepository,
                               SolicitudMaterialRepository solicitudRepository) {
        this.ordenService = ordenService;
        this.ordenRepository = ordenRepository;
        this.citaRepository = citaRepository;
        this.tareaRepository = tareaRepository;
        this.solicitudRepository = solicitudRepository;
    }
    
    @Transactional
    public FinalizacionResponse finalizar(String correoMecanico, Long idOrden) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        ordenService.validarEnProceso(orden);

        if (orden.getDiagnostico() == null) {
            throw new ConflictException("Registra el diagnostico antes de finalizar la orden");
        }
        List<TareaOrden> tareas = tareaRepository.findByOrden_IdOrdenOrderByIdTareaAsc(idOrden);
        if (tareas.isEmpty()) {
            throw new ConflictException("Registra al menos una tarea antes de finalizar la orden");
        }
        long pendientes = tareas.stream().filter(t -> !t.isCompletada()).count();
        if (pendientes > 0) {
            throw new ConflictException("Faltan " + pendientes + " de " + tareas.size() + " tareas por completar");
        }
        if (solicitudRepository.existsByOrden_IdOrdenAndEstado(idOrden, EstadoSolicitud.PENDIENTE)) {
            throw new ConflictException("Hay solicitudes de repuestos pendientes de atencion");
        }

        orden.setFechaFin(LocalDateTime.now(ZONA_LIMA).withNano(0));
        ordenRepository.save(orden);

        Cita cita = orden.getCita();
        cita.setEstado(EstadoCita.FINALIZADA);
        citaRepository.save(cita);

        return new FinalizacionResponse(
                orden.getIdOrden(),
                cita.getIdCita(),
                cita.getTicket(),
                cita.getEstado().name(),
                orden.getFechaInicio().toString(),
                orden.getFechaFin().toString(),
                orden.getCostoRepuestos());
    }
}