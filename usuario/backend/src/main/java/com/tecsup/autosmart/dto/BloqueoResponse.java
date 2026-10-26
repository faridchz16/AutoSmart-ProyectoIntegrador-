package com.tecsup.autosmart.dto;

public class BloqueoResponse {

    private Long idBloqueo;
    private String fecha;
    private String horaInicio;
    private String horaFin;
    private String expiraEn;
    private long segundosRestantes;

    public Long getIdBloqueo() { return idBloqueo; }
    public void setIdBloqueo(Long idBloqueo) { this.idBloqueo = idBloqueo; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }
    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }
    public String getExpiraEn() { return expiraEn; }
    public void setExpiraEn(String expiraEn) { this.expiraEn = expiraEn; }
    public long getSegundosRestantes() { return segundosRestantes; }
    public void setSegundosRestantes(long segundosRestantes) { this.segundosRestantes = segundosRestantes; }
}