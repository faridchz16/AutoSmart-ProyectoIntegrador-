package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.PresupuestoRequest;
import com.tecsup.autosmart.dto.PresupuestoResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.Presupuesto;
import com.tecsup.autosmart.model.PresupuestoItem;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import com.tecsup.autosmart.repository.PresupuestoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Service
public class PresupuestoService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final PresupuestoRepository presupuestoRepository;
    private final OrdenTrabajoRepository ordenRepository;
    private final CitaRepository citaRepository;
    private final OrdenTrabajoService ordenService;
    private final DeficienciaService deficienciaService;

    public PresupuestoService(PresupuestoRepository presupuestoRepository,
                              OrdenTrabajoRepository ordenRepository,
                              CitaRepository citaRepository,
                              OrdenTrabajoService ordenService,
                              DeficienciaService deficienciaService) {
        this.presupuestoRepository = presupuestoRepository;
        this.ordenRepository = ordenRepository;
        this.citaRepository = citaRepository;
        this.ordenService = ordenService;
        this.deficienciaService = deficienciaService;
    }

    @Transactional
    public PresupuestoResponse crear(String correoMecanico, Long idOrden, PresupuestoRequest req) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        ordenService.validarEnProceso(orden);
        if (orden.getDiagnostico() == null) {
            throw new ConflictException("Registra el diagnostico antes de generar el presupuesto");
        }

        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setOrden(orden);
        presupuesto.setDescripcion(req.getDescripcion().trim());
        presupuesto.setFechaCreacion(LocalDateTime.now(ZONA_LIMA).withNano(0));

        BigDecimal total = BigDecimal.ZERO;
        for (PresupuestoRequest.Item datos : req.getItems()) {
            BigDecimal precio = datos.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(datos.getCantidad()));

            PresupuestoItem item = new PresupuestoItem();
            item.setPresupuesto(presupuesto);
            item.setTipo(datos.getTipo());
            item.setDescripcion(datos.getDescripcion().trim());
            item.setCantidad(datos.getCantidad());
            item.setPrecioUnitario(precio);
            item.setSubtotal(subtotal);
            presupuesto.getItems().add(item);

            total = total.add(subtotal);
        }
        presupuesto.setTotal(total);

        Cita cita = orden.getCita();
        cita.setEstado(EstadoCita.ESPERA_APROBACION);
        citaRepository.save(cita);

        return aRespuesta(presupuestoRepository.save(presupuesto));
    }

    @Transactional(readOnly = true)
    public PresupuestoResponse obtenerParaMecanico(String correoMecanico, Long idOrden) {
        ordenService.ordenDelMecanico(correoMecanico, idOrden);
        return aRespuesta(ultimoDeLaOrden(idOrden));
    }

    @Transactional(readOnly = true)
    public PresupuestoResponse obtenerParaCliente(String correoCliente, Long idCita) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        if (!cita.getCliente().getCorreo().equalsIgnoreCase(correoCliente)) {
            throw new ForbiddenException("Solo el cliente de la cita puede ver su presupuesto");
        }
        OrdenTrabajo orden = ordenRepository.findByCita_IdCita(idCita)
                .orElseThrow(() -> new NotFoundException("La cita aun no tiene presupuesto"));
        return aRespuesta(ultimoDeLaOrden(orden.getIdOrden()));
    }

    private Presupuesto ultimoDeLaOrden(Long idOrden) {
        return presupuestoRepository.findFirstByOrden_IdOrdenOrderByIdPresupuestoDesc(idOrden)
                .orElseThrow(() -> new NotFoundException("La orden aun no tiene presupuesto"));
    }

    private PresupuestoResponse aRespuesta(Presupuesto p) {
        PresupuestoResponse res = new PresupuestoResponse();
        res.setIdPresupuesto(p.getIdPresupuesto());
        res.setIdOrden(p.getOrden().getIdOrden());
        res.setIdCita(p.getOrden().getCita().getIdCita());
        res.setEstado(p.getEstado().name());
        res.setDescripcion(p.getDescripcion());
        res.setFechaCreacion(p.getFechaCreacion().toString());
        res.setFechaDecision(p.getFechaDecision() == null ? null : p.getFechaDecision().toString());
        res.setItems(p.getItems().stream()
                .map(i -> new PresupuestoResponse.Item(i.getTipo().name(), i.getDescripcion(),
                        i.getCantidad(), i.getPrecioUnitario(), i.getSubtotal()))
                .collect(Collectors.toList()));
        res.setTotal(p.getTotal());
        res.setDeficiencias(deficienciaService.listarPorOrden(p.getOrden().getIdOrden()));
        return res;
    }
}