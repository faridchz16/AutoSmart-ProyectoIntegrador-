package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TareaRequest {

    @NotBlank(message = "La descripcion de la tarea es obligatoria")
    @Size(max = 200, message = "La descripcion no puede superar 200 caracteres")
    private String descripcion;

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}