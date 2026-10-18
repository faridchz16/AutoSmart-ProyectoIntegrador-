package com.tecsup.autosmart.dto;

public class FechaBloqueadaResponse {

    private Integer idBloqueo;
    private String fecha;
    private String motivo;

    public Integer getIdBloqueo() { return idBloqueo; }
    public void setIdBloqueo(Integer idBloqueo) { this.idBloqueo = idBloqueo; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}