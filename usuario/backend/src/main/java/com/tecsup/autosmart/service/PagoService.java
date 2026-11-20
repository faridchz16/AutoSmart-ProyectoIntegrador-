package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.PagoRequest;
import com.tecsup.autosmart.dto.PagoResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.mapper.PagoMapper;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.EstadoPago;
import com.tecsup.autosmart.model.MetodoPago;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.Pago;
import com.tecsup.autosmart.pasarela.PasarelaPago;
import com.tecsup.autosmart.pasarela.ResultadoPasarela;
import com.tecsup.autosmart.pasarela.SolicitudCobro;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.MetodoPagoRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import com.tecsup.autosmart.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PagoService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final OrdenTrabajoRepository ordenRepository;
    private final CitaRepository citaRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final PagoRepository pagoRepository;
    private final CostoOrdenService costoOrdenService;
    private final PagoMapper mapper;
    private final Map<String, PasarelaPago> pasarelas;

    public PagoService(OrdenTrabajoRepository ordenRepository,
                       CitaRepository citaRepository,
                       MetodoPagoRepository metodoPagoRepository,
                       PagoRepository pagoRepository,
                       CostoOrdenService costoOrdenService,
                       PagoMapper mapper,
                       List<PasarelaPago> pasarelas) {
        this.ordenRepository = ordenRepository;
        this.citaRepository = citaRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.pagoRepository = pagoRepository;
        this.costoOrdenService = costoOrdenService;
        this.mapper = mapper;
        this.pasarelas = pasarelas.stream().collect(Collectors.toMap(PasarelaPago::metodo, Function.identity()));
    }

    @Transactional
    public PagoResponse pagar(String correoCliente, PagoRequest req) {
        OrdenTrabajo orden = ordenRepository.findById(req.idOrden())
                .orElseThrow(() -> new NotFoundException("Orden de trabajo no encontrada"));
        Cita cita = orden.getCita();
        if (!cita.getCliente().getCorreo().equalsIgnoreCase(correoCliente)) {
            throw new ForbiddenException("Esta orden no pertenece a tu cuenta");
        }

        MetodoPago metodo = metodoHabilitado(req.metodo());
        if (!Boolean.TRUE.equals(metodo.getEnLinea())) {
            throw new BadRequestException("El pago en efectivo se registra en el taller");
        }
        PasarelaPago pasarela = pasarelas.get(metodo.getCodigo());
        if (pasarela == null) {
            throw new BadRequestException("El metodo " + metodo.getCodigo() + " no esta disponible en linea");
        }

        validarPagable(cita, orden.getIdOrden());
        BigDecimal total = costoOrdenService.costo(correoCliente, orden.getIdOrden()).total();

        ResultadoPasarela resultado = pasarela.cobrar(
                new SolicitudCobro(total, req.tokenPago().trim(), "AutoSmart " + cita.getTicket()));

        Pago pago = new Pago();
        pago.setOrden(orden);
        pago.setMetodo(metodo);
        pago.setMonto(total);
        pago.setEstado(resultado.aprobado() ? EstadoPago.APROBADO : EstadoPago.RECHAZADO);
        pago.setMotivoRechazo(resultado.motivoRechazo());
        pago.setReferenciaExterna(resultado.referencia());
        pago = registrar(pago, null);

        if (resultado.aprobado()) {
            cita.cambiarEstado(EstadoCita.PAGADO);
            citaRepository.save(cita);
        }
        return mapper.toResponse(pago);
    }

    public MetodoPago metodoHabilitado(String codigo) {
        return metodoPagoRepository.findByCodigoAndHabilitadoTrue(codigo.trim().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Metodo de pago no valido: " + codigo));
    }

    public void validarPagable(Cita cita, Long idOrden) {
        if (pagoRepository.existsByOrden_IdOrdenAndEstado(idOrden, EstadoPago.APROBADO)) {
            throw new ConflictException("La orden ya fue pagada");
        }
        if (cita.getEstado() != EstadoCita.FINALIZADA) {
            throw new ConflictException("Solo se puede pagar una orden FINALIZADA (estado: " + cita.getEstado() + ")");
        }
    }

    public Pago registrar(Pago pago, LocalDateTime fecha) {
        pago.setFecha(fecha == null ? LocalDateTime.now(LIMA).withNano(0) : fecha.withNano(0));
        pago.setCodigo("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        pago = pagoRepository.saveAndFlush(pago);
        pago.setCodigo(String.format("PAG-%d-%05d", pago.getFecha().getYear(), pago.getIdPago()));
        return pagoRepository.save(pago);
    }
}