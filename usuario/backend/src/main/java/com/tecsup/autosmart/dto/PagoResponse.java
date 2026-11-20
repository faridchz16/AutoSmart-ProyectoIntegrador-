package com.tecsup.autosmart.dto;

import java.math.BigDecimal;

public record PagoResponse(
        Long idPago,
        String codigo,
        Long idOrden,
        String ticket,
        String metodo,
        String estado,
        BigDecimal monto,
        String fecha,
        String motivoRechazo,
        String comprobanteUrl,
        String registradoPor) {}