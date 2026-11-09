package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.DetalleMantenimientoResponse;
import com.tecsup.autosmart.dto.DetalleMantenimientoResponse.ItemChecklist;
import com.tecsup.autosmart.dto.DetalleMantenimientoResponse.ResumenPresupuesto;
import com.tecsup.autosmart.dto.PresupuestoResponse;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.ChecklistItem;
import com.tecsup.autosmart.model.EstadoPresupuesto;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.Presupuesto;
import com.tecsup.autosmart.repository.ChecklistRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import com.tecsup.autosmart.repository.PresupuestoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class DetalleMantenimientoService {

    private final OrdenTrabajoRepository ordenRepository;
    private final ChecklistRepository checklistRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final DeficienciaService deficienciaService;
    private final TareaOrdenService tareaService;
    private final KardexService kardexService;

    public DetalleMantenimientoService(OrdenTrabajoRepository ordenRepository,
                                       ChecklistRepository checklistRepository,
                                       PresupuestoRepository presupuestoRepository,
                                       DeficienciaService deficienciaService,
                                       TareaOrdenService tareaService,
                                       KardexService kardexService) {
        this.ordenRepository = ordenRepository;
        this.checklistRepository = checklistRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.deficienciaService = deficienciaService;
        this.tareaService = tareaService;
        this.kardexService = kardexService;
    }

    @Transactional(readOnly = true)
    public DetalleMantenimientoResponse detalle(String correoUsuario, Long idOrden) {
        OrdenTrabajo orden = ordenRepository.findById(idOrden)
                .orElseThrow(() -> new NotFoundException("Orden de trabajo no encontrada"));
        Cita cita = orden.getCita();
        boolean esCliente = cita.getCliente().getCorreo().equalsIgnoreCase(correoUsuario);
        boolean esMecanico = cita.getMecanico().getCorreo().equalsIgnoreCase(correoUsuario);
        if (!esCliente && !esMecanico) {
            throw new ForbiddenException("No puedes ver el detalle de este mantenimiento");
        }

        DetalleMantenimientoResponse res = new DetalleMantenimientoResponse();
        res.setIdOrden(orden.getIdOrden());
        res.setIdCita(cita.getIdCita());
        res.setTicket(cita.getTicket());
        res.setEstado(cita.getEstado().name());
        res.setServicio(cita.getServicio().getNombre());
        res.setPrecioServicio(cita.getPrecio());
        res.setPlaca(cita.getVehiculo().getPlaca());
        res.setVehiculo(cita.getVehiculo().getMarca() + " " + cita.getVehiculo().getModelo());
        res.setMecanico(cita.getMecanico().getNombre());
        res.setFechaInicio(orden.getFechaInicio().toString());
        res.setFechaFin(orden.getFechaFin() == null ? null : orden.getFechaFin().toString());
        res.setDiagnostico(orden.getDiagnostico());

        res.setChecklist(checklistRepository.findByOrden_IdOrden(idOrden)
                .map(c -> c.getItems().stream()
                        .sorted(Comparator.comparing(ChecklistItem::getElemento))
                        .map(i -> new ItemChecklist(i.getElemento().name(), i.getEstado().name(), i.getObservacion()))
                        .collect(Collectors.toList()))
                .orElse(null));
        res.setDeficiencias(deficienciaService.listarPorOrden(idOrden));
        res.setTareas(tareaService.resumen(idOrden));
        res.setRepuestosConsumidos(kardexService.consumosDeOrden(idOrden));
        res.setCostoRepuestos(orden.getCostoRepuestos());

        Presupuesto presupuesto = presupuestoRepository.findFirstByOrden_IdOrdenOrderByIdPresupuestoDesc(idOrden)
                .orElse(null);
        res.setPresupuesto(presupuesto == null ? null : resumen(presupuesto));

        BigDecimal aprobado = (presupuesto != null && presupuesto.getEstado() == EstadoPresupuesto.APROBADO)
                ? presupuesto.getTotal() : BigDecimal.ZERO;
        res.setTotalEstimado(cita.getPrecio().add(aprobado));
        return res;
    }

    private ResumenPresupuesto resumen(Presupuesto p) {
        return new ResumenPresupuesto(
                p.getIdPresupuesto(),
                p.getEstado().name(),
                p.getDescripcion(),
                p.getTotal(),
                p.getItems().stream()
                        .map(i -> new PresupuestoResponse.Item(i.getTipo().name(), i.getDescripcion(),
                                i.getCantidad(), i.getPrecioUnitario(), i.getSubtotal()))
                        .collect(Collectors.toList()));
    }
}