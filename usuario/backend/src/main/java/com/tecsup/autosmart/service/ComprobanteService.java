package com.tecsup.autosmart.service;

import com.tecsup.autosmart.comprobante.ComprobantePdf;
import com.tecsup.autosmart.comprobante.GeneradorComprobante;
import com.tecsup.autosmart.dto.CostoOrdenResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.EstadoPago;
import com.tecsup.autosmart.model.Pago;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComprobanteService {

    private final PagoRepository pagoRepository;
    private final CostoOrdenService costoOrdenService;
    private final GeneradorComprobante generador;

    public ComprobanteService(PagoRepository pagoRepository,
                              CostoOrdenService costoOrdenService,
                              GeneradorComprobante generador) {
        this.pagoRepository = pagoRepository;
        this.costoOrdenService = costoOrdenService;
        this.generador = generador;
    }

    @Transactional(readOnly = true)
    public ComprobantePdf generar(String correo, boolean esAdmin, Long idPago) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));

        if (pago.getEstado() != EstadoPago.APROBADO) {
            throw new ConflictException("Solo los pagos aprobados tienen comprobante");
        }

        Usuario cliente = pago.getOrden().getCita().getCliente();
        if (!esAdmin && (cliente == null || !cliente.getCorreo().equalsIgnoreCase(correo))) {
            throw new ForbiddenException("Este pago no pertenece a tu cuenta");
        }

        CostoOrdenResponse costo = costoOrdenService.calcular(pago.getOrden());
        byte[] contenido = generador.generar(pago, costo);
        return new ComprobantePdf("comprobante-" + pago.getCodigo() + ".pdf", contenido);
    }
}