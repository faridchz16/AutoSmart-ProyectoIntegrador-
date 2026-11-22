package com.tecsup.autosmart.dto;

import java.math.BigDecimal;

public record PagoAdminResponse(
        Long idPago,
        String codigo,
        String fecha,
        String ticket,
        String cliente,
        String placa,
        String metodo,
        String estado,
        BigDecimal monto,
        String registradoPor
) {
}