package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DiagnosticoRequest {

    @NotBlank(message = "El diagnostico es obligatorio")
    @Size(max = 2000, message = "El diagnostico no puede superar 2000 caracteres")
    private String diagnostico;

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
}