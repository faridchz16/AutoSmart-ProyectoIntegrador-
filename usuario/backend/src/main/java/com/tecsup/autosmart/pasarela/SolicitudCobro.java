package com.tecsup.autosmart.pasarela;

import java.math.BigDecimal;

public record SolicitudCobro(BigDecimal monto, String tokenPago, String descripcion) {}