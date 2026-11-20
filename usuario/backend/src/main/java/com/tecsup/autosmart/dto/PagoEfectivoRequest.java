package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoEfectivoRequest(
        @NotNull(message = "El idOrden es obligatorio") Long idOrden,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0") BigDecimal monto,
        LocalDateTime fecha,
        @Size(max = 200, message = "La observacion admite como maximo 200 caracteres") String observacion) {}