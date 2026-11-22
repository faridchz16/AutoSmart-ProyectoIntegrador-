package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FaqRequest(
        @NotBlank(message = "La pregunta es obligatoria")
        @Size(max = 255, message = "La pregunta no puede superar los 255 caracteres")
        String pregunta,

        @NotBlank(message = "La respuesta es obligatoria")
        @Size(max = 1000, message = "La respuesta no puede superar los 1000 caracteres")
        String respuesta,

        @Size(max = 255, message = "Las palabras clave no pueden superar los 255 caracteres")
        String palabrasClave
) {
}