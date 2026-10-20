package com.tecsup.autosmart.dto;

import java.time.LocalTime;

public class TurnoResponse {

    private LocalTime horaInicio;
    private LocalTime horaFin;

    public TurnoResponse(LocalTime horaInicio, LocalTime horaFin) {
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
}