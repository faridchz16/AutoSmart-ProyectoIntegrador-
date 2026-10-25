package com.tecsup.autosmart.dto;

public class DiaTallerDto {

    private Integer diaSemana;
    private Boolean abierto;
    private String horaApertura;
    private String horaCierre;

    public Integer getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Integer diaSemana) { this.diaSemana = diaSemana; }
    public Boolean getAbierto() { return abierto; }
    public void setAbierto(Boolean abierto) { this.abierto = abierto; }
    public String getHoraApertura() { return horaApertura; }
    public void setHoraApertura(String horaApertura) { this.horaApertura = horaApertura; }
    public String getHoraCierre() { return horaCierre; }
    public void setHoraCierre(String horaCierre) { this.horaCierre = horaCierre; }
}