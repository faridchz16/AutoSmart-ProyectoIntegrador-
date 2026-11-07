package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotNull;

public class TareaEstadoRequest {

    @NotNull(message = "Indica si la tarea esta completada (true o false)")
    private Boolean completada;

    public Boolean getCompletada() { return completada; }
    public void setCompletada(Boolean completada) { this.completada = completada; }
}