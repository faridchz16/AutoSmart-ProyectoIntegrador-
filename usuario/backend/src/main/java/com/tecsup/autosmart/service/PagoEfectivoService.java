package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.PagoEfectivoRequest;
import com.tecsup.autosmart.dto.PagoResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.mapper.PagoMapper;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.EstadoPago;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.Pago;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class PagoEfectivoService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final OrdenTrabajoRepository ordenRepository;
    private final CitaRepository citaRepository;
    private final PagoService pagoService;
    private final CostoOrdenService costoOrdenService;
    private final PagoMapper mapper;

    public PagoEfectivoService(OrdenTrabajoRepository ordenRepository,
                               CitaRepository citaRepository,
                               PagoService pagoService,
                               CostoOrdenService costoOrdenService,
                               PagoMapper mapper) {
        this.ordenRepository = ordenRepository;
        this.citaRepository = citaRepository;
        this.pagoService = pagoService;
        this.costoOrdenService = costoOrdenService;
        this.mapper = mapper;
    }

    @Transactional
    public PagoResponse registrar(String correoAdmin, PagoEfectivoRequest req) {
        OrdenTrabajo orden = ordenRepository.findById(req.idOrden())
                .orElseThrow(() -> new NotFoundException("Orden de trabajo no encontrada"));
        Cita cita = orden.getCita();

        pagoService.validarPagable(cita, orden.getIdOrden());

        BigDecimal total = costoOrdenService.costo(cita.getCliente().getCorreo(), orden.getIdOrden()).total();
        if (req.monto().compareTo(total) < 0) {
            throw new BadRequestException("El monto recibido es menor al total de la orden (S/ " + total + ")");
        }
        if (req.fecha() != null && req.fecha().isAfter(LocalDateTime.now(LIMA))) {
            throw new BadRequestException("La fecha del pago no puede ser futura");
        }

        Pago pago = new Pago();
        pago.setOrden(orden);
        pago.setMetodo(pagoService.metodoHabilitado("EFECTIVO"));
        pago.setMonto(total);
        pago.setEstado(EstadoPago.APROBADO);
        pago.setRegistradoPor(correoAdmin);
        pago.setObservacion(observacion(req, total));
        pago = pagoService.registrar(pago, req.fecha());

        cita.cambiarEstado(EstadoCita.PAGADO);
        citaRepository.save(cita);

        return mapper.toResponse(pago);
    }

    private String observacion(PagoEfectivoRequest req, BigDecimal total) {
        String base = req.observacion() == null ? "" : req.observacion().trim();
        BigDecimal vuelto = req.monto().subtract(total);
        if (vuelto.signum() == 0) {
            return base.isEmpty() ? null : base;
        }
        String detalle = "Recibido S/ " + req.monto() + ", vuelto S/ " + vuelto;
        String texto = base.isEmpty() ? detalle : base + " (" + detalle + ")";
        return texto.length() > 200 ? texto.substring(0, 200) : texto;
    }
}