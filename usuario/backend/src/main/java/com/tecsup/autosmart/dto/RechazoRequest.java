package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RechazoRequest {

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
    private String motivo;

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}