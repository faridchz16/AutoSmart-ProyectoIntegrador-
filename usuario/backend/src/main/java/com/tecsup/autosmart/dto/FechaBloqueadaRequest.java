package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class FechaBloqueadaRequest {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
    private String motivo;

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}