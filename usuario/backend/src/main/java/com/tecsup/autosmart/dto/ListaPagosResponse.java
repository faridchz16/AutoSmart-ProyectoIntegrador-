package com.tecsup.autosmart.dto;

import java.math.BigDecimal;
import java.util.List;

public record ListaPagosResponse(
        String desde,
        String hasta,
        int cantidad,
        BigDecimal totalAprobado,
        List<PagoAdminResponse> pagos
) {
}