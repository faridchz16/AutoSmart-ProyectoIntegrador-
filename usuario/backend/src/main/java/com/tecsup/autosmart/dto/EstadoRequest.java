package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotNull;

public class EstadoRequest {

    @NotNull(message = "El campo activo es obligatorio")
    private Boolean activo;

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}