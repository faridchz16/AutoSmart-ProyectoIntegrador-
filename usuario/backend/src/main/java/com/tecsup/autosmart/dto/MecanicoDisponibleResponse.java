package com.tecsup.autosmart.dto;

import java.util.List;

public class MecanicoDisponibleResponse {

    private Integer idMecanico;
    private String nombre;
    private Integer aniosExperiencia;
    private List<String> especialidades;

    public Integer getIdMecanico() { return idMecanico; }
    public void setIdMecanico(Integer idMecanico) { this.idMecanico = idMecanico; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getAniosExperiencia() { return aniosExperiencia; }
    public void setAniosExperiencia(Integer aniosExperiencia) { this.aniosExperiencia = aniosExperiencia; }
    public List<String> getEspecialidades() { return especialidades; }
    public void setEspecialidades(List<String> especialidades) { this.especialidades = especialidades; }
}