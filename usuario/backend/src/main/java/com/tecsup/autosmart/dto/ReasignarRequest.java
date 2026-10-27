package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotNull;

public class ReasignarRequest {

    @NotNull(message = "El mecanico es obligatorio")
    private Integer idMecanico;

    public Integer getIdMecanico() { return idMecanico; }
    public void setIdMecanico(Integer idMecanico) { this.idMecanico = idMecanico; }
}