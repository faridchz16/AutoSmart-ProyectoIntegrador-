package com.tecsup.autosmart.model;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "configuracion_taller")
public class ConfiguracionTaller {

    @Id
    @Column(name = "dia_semana")
    private Integer diaSemana; // 1 = lunes ... 7 = domingo

    @Column(name = "abierto", nullable = false)
    private Boolean abierto;

    @Column(name = "hora_apertura", nullable = false)
    private LocalTime horaApertura;

    @Column(name = "hora_cierre", nullable = false)
    private LocalTime horaCierre;

    public ConfiguracionTaller() {}

    public ConfiguracionTaller(Integer diaSemana, Boolean abierto, LocalTime horaApertura, LocalTime horaCierre) {
        this.diaSemana = diaSemana;
        this.abierto = abierto;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
    }

    public Integer getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Integer diaSemana) { this.diaSemana = diaSemana; }
    public Boolean getAbierto() { return abierto; }
    public void setAbierto(Boolean abierto) { this.abierto = abierto; }
    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }
    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }
}