package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CostoOrdenResponse;
import com.tecsup.autosmart.dto.CostoOrdenResponse.Adicional;
import com.tecsup.autosmart.dto.CostoOrdenResponse.PagoRealizado;
import com.tecsup.autosmart.dto.CostoOrdenResponse.RepuestoUtilizado;
import com.tecsup.autosmart.dto.CostoOrdenResponse.ServicioCobrado;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.mapper.CostoOrdenMapper;
import com.tecsup.autosmart.mapper.PagoMapper;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.EstadoPago;
import com.tecsup.autosmart.model.EstadoPresupuesto;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.TipoMovimiento;
import com.tecsup.autosmart.repository.MovimientoInventarioRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import com.tecsup.autosmart.repository.PagoRepository;
import com.tecsup.autosmart.repository.PresupuestoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CostoOrdenService {

    private final OrdenTrabajoRepository ordenRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final PagoRepository pagoRepository;
    private final CostoOrdenMapper mapper;
    private final PagoMapper pagoMapper;

    public CostoOrdenService(OrdenTrabajoRepository ordenRepository,
                             PresupuestoRepository presupuestoRepository,
                             MovimientoInventarioRepository movimientoRepository,
                             PagoRepository pagoRepository,
                             CostoOrdenMapper mapper,
                             PagoMapper pagoMapper) {
        this.ordenRepository = ordenRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.movimientoRepository = movimientoRepository;
        this.pagoRepository = pagoRepository;
        this.mapper = mapper;
        this.pagoMapper = pagoMapper;
    }

    @Transactional(readOnly = true)
    public CostoOrdenResponse costo(String correoCliente, Long idOrden) {
        OrdenTrabajo orden = ordenRepository.findById(idOrden)
                .orElseThrow(() -> new NotFoundException("Orden de trabajo no encontrada"));
        Cita cita = orden.getCita();
        if (!cita.getCliente().getCorreo().equalsIgnoreCase(correoCliente)) {
            throw new ForbiddenException("Esta orden no pertenece a tu cuenta");
        }

        List<Adicional> adicionales = presupuestoRepository
                .findFirstByOrden_IdOrdenOrderByIdPresupuestoDesc(idOrden)
                .filter(p -> p.getEstado() == EstadoPresupuesto.APROBADO)
                .map(p -> p.getItems().stream().map(mapper::toAdicional).toList())
                .orElse(List.of());

        BigDecimal totalAdicionales = adicionales.stream()
                .map(Adicional::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RepuestoUtilizado> repuestos = movimientoRepository
                .findByOrden_IdOrdenAndTipoOrderByIdMovimientoAsc(idOrden, TipoMovimiento.SALIDA)
                .stream().map(mapper::toRepuesto).toList();

        BigDecimal precioServicio = cita.getPrecio() == null ? BigDecimal.ZERO : cita.getPrecio();
        BigDecimal total = precioServicio.add(totalAdicionales);

        PagoRealizado pago = pagoRepository.findFirstByOrden_IdOrdenAndEstado(idOrden, EstadoPago.APROBADO)
                .map(pagoMapper::toPagoRealizado)
                .orElse(null);
        boolean puedePagar = cita.getEstado() == EstadoCita.FINALIZADA && pago == null;

        return new CostoOrdenResponse(
                orden.getIdOrden(),
                cita.getIdCita(),
                cita.getTicket(),
                cita.getEstado().name(),
                new ServicioCobrado(cita.getServicio().getNombre(), precioServicio),
                adicionales,
                totalAdicionales,
                repuestos,
                total,
                "PEN",
                puedePagar,
                pago);
    }
}