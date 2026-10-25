package com.tecsup.autosmart.dto;

import java.util.List;

public class MecanicoLibreResponse {

    private Integer idUsuario;
    private String nombre;
    private List<String> especialidades;

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public List<String> getEspecialidades() { return especialidades; }
    public void setEspecialidades(List<String> especialidades) { this.especialidades = especialidades; }
}