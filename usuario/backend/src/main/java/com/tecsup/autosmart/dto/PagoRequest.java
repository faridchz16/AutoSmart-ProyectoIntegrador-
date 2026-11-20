package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PagoRequest(
        @NotNull(message = "El idOrden es obligatorio") Long idOrden,
        @NotBlank(message = "El metodo es obligatorio") String metodo,
        @NotBlank(message = "El tokenPago es obligatorio") @Size(max = 100) String tokenPago) {}