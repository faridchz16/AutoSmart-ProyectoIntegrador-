package com.tecsup.autosmart.dto;

import java.util.List;

public class PerfilMecanicoResponse {

    private Integer idUsuario;
    private String nombre;
    private String correo;
    private Integer aniosExperiencia;
    private List<String> especialidades;

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public Integer getAniosExperiencia() { return aniosExperiencia; }
    public void setAniosExperiencia(Integer aniosExperiencia) { this.aniosExperiencia = aniosExperiencia; }

    public List<String> getEspecialidades() { return especialidades; }
    public void setEspecialidades(List<String> especialidades) { this.especialidades = especialidades; }
}