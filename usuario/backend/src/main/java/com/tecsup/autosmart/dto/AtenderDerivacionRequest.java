package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtenderDerivacionRequest(
        @NotBlank(message = "La nota es obligatoria")
        @Size(max = 500, message = "La nota no puede superar los 500 caracteres")
        String nota
) {
}