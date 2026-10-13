package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PerfilMecanicoRequest {

    @NotNull(message = "Los anios de experiencia son obligatorios")
    @Min(value = 0, message = "Los anios de experiencia no pueden ser negativos")
    @Max(value = 60, message = "Los anios de experiencia no pueden superar 60")
    private Integer aniosExperiencia;

    @NotNull(message = "Las especialidades son obligatorias")
    @Size(max = 10, message = "Puedes registrar hasta 10 especialidades")
    private List<String> especialidades;

    public Integer getAniosExperiencia() { return aniosExperiencia; }
    public void setAniosExperiencia(Integer aniosExperiencia) { this.aniosExperiencia = aniosExperiencia; }

    public List<String> getEspecialidades() { return especialidades; }
    public void setEspecialidades(List<String> especialidades) { this.especialidades = especialidades; }
}