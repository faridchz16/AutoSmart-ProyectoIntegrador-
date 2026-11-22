package com.tecsup.autosmart.mapper;

import com.tecsup.autosmart.dto.CostoOrdenResponse.PagoRealizado;
import com.tecsup.autosmart.dto.PagoAdminResponse;
import com.tecsup.autosmart.dto.PagoResponse;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoPago;
import com.tecsup.autosmart.model.Pago;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    public PagoResponse toResponse(Pago pago) {
        boolean aprobado = pago.getEstado() == EstadoPago.APROBADO;
        return new PagoResponse(
                pago.getIdPago(),
                pago.getCodigo(),
                pago.getOrden().getIdOrden(),
                pago.getOrden().getCita().getTicket(),
                pago.getMetodo().getCodigo(),
                pago.getEstado().name(),
                pago.getMonto(),
                pago.getFecha().toString(),
                pago.getMotivoRechazo(),
                aprobado ? "/payments/" + pago.getIdPago() + "/receipt" : null,
                pago.getRegistradoPor());
    }

    public PagoRealizado toPagoRealizado(Pago pago) {
        return new PagoRealizado(pago.getIdPago(), pago.getCodigo(), pago.getMetodo().getCodigo(),
                pago.getFecha().toString());
    }

    public PagoAdminResponse toAdmin(Pago pago) {
        Cita cita = pago.getOrden().getCita();
        return new PagoAdminResponse(
                pago.getIdPago(),
                pago.getCodigo(),
                pago.getFecha().toString(),
                cita.getTicket(),
                cita.getCliente() != null ? cita.getCliente().getNombre() : null,
                cita.getVehiculo() != null ? cita.getVehiculo().getPlaca() : null,
                pago.getMetodo().getCodigo(),
                pago.getEstado().name(),
                pago.getMonto(),
                pago.getRegistradoPor());
    }
}