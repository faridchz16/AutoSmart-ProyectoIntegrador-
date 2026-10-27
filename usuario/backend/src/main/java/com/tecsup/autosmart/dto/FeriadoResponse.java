package com.tecsup.autosmart.dto;

public class FeriadoResponse {

    private Integer idFeriado;
    private String fecha;
    private String descripcion;

    public Integer getIdFeriado() { return idFeriado; }
    public void setIdFeriado(Integer idFeriado) { this.idFeriado = idFeriado; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}