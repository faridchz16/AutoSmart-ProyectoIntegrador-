package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.SeguimientoResponse;
import com.tecsup.autosmart.dto.SeguimientoResponse.Hito;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.Presupuesto;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import com.tecsup.autosmart.repository.PresupuestoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SeguimientoService {

    private final CitaRepository citaRepository;
    private final OrdenTrabajoRepository ordenRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final TareaOrdenService tareaService;

    public SeguimientoService(CitaRepository citaRepository,
                              OrdenTrabajoRepository ordenRepository,
                              PresupuestoRepository presupuestoRepository,
                              TareaOrdenService tareaService) {
        this.citaRepository = citaRepository;
        this.ordenRepository = ordenRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.tareaService = tareaService;
    }

    // T53.1: linea de tiempo de la cita para el cliente duenio
    @Transactional(readOnly = true)
    public SeguimientoResponse seguimiento(String correoCliente, Long idCita) {
        Cita cita = citaRepository.buscarDetalle(idCita)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        if (!cita.getCliente().getCorreo().equalsIgnoreCase(correoCliente)) {
            throw new ForbiddenException("Solo el cliente de la cita puede ver su seguimiento");
        }

        EstadoCita estado = cita.getEstado();
        Optional<OrdenTrabajo> orden = ordenRepository.findByCita_IdCita(idCita);

        SeguimientoResponse res = new SeguimientoResponse();
        res.setIdCita(cita.getIdCita());
        res.setTicket(cita.getTicket());
        res.setEstado(estado.name());
        res.setServicio(cita.getServicio().getNombre());
        res.setPlaca(cita.getVehiculo().getPlaca());
        res.setMecanico(cita.getMecanico().getNombre());
        res.setFecha(cita.getFecha().toString());
        res.setHoraInicio(cita.getHoraInicio().toString());
        res.setPorcentajeAvance(orden.map(o -> tareaService.resumen(o.getIdOrden()).getPorcentajeAvance()).orElse(0));
        res.setLineaDeTiempo(construirLinea(cita, estado, orden));
        return res;
    }

    private List<Hito> construirLinea(Cita cita, EstadoCita estado, Optional<OrdenTrabajo> ordenOpt) {
        List<Hito> linea = new ArrayList<>();
        linea.add(new Hito("RESERVADA", "Cita reservada", true, null,
                "Turno del " + cita.getFecha() + " a las " + cita.getHoraInicio()));

        if (estado == EstadoCita.RECHAZADA || estado == EstadoCita.CANCELADA) {
            String titulo = estado == EstadoCita.RECHAZADA ? "Cita rechazada" : "Cita cancelada";
            linea.add(new Hito(estado.name(), titulo, true, null, cita.getMotivoRechazo()));
            return linea;
        }

        boolean confirmada = estado != EstadoCita.PENDIENTE;
        linea.add(new Hito("CONFIRMADA", "Cita confirmada por el mecánico", confirmada, null, null));

        OrdenTrabajo orden = ordenOpt.orElse(null);
        linea.add(new Hito("RECIBIDO", "Vehículo recibido en el taller", orden != null,
                orden == null ? null : texto(orden.getFechaInicio()), null));
        linea.add(new Hito("DIAGNOSTICO", "Diagnóstico registrado", orden != null && orden.getDiagnostico() != null,
                orden == null ? null : texto(orden.getFechaDiagnostico()), null));

        if (orden != null) {
            presupuestoRepository.findFirstByOrden_IdOrdenOrderByIdPresupuestoDesc(orden.getIdOrden())
                    .ifPresent(p -> linea.add(hitoPresupuesto(p)));
        }

        boolean finalizada = estado == EstadoCita.FINALIZADA;
        linea.add(new Hito("EN_REPARACION", "Reparación en curso", finalizada,
                null, orden == null ? null : "Avance: " + tareaService.resumen(orden.getIdOrden()).getPorcentajeAvance() + " %"));
        linea.add(new Hito("FINALIZADA", "Vehículo listo para recoger", finalizada,
                orden == null ? null : texto(orden.getFechaFin()), null));
        return linea;
    }

    private Hito hitoPresupuesto(Presupuesto p) {
        boolean decidido = p.getFechaDecision() != null;
        String detalle = "Total S/ " + p.getTotal() + " - " + p.getEstado().name();
        return new Hito("PRESUPUESTO", decidido ? "Presupuesto respondido" : "Presupuesto enviado (espera tu aprobación)",
                decidido, texto(decidido ? p.getFechaDecision() : p.getFechaCreacion()), detalle);
    }

    private String texto(LocalDateTime fecha) {
        return fecha == null ? null : fecha.toString();
    }
}