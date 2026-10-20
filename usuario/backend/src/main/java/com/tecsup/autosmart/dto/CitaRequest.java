package com.tecsup.autosmart.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class CitaRequest {

    @NotNull(message = "El servicio es obligatorio")
    private Long idServicio;

    @NotNull(message = "El mecanico es obligatorio")
    private Integer idMecanico;

    @NotNull(message = "El vehiculo es obligatorio")
    private Long idVehiculo;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    public Long getIdServicio() { return idServicio; }
    public void setIdServicio(Long idServicio) { this.idServicio = idServicio; }
    public Integer getIdMecanico() { return idMecanico; }
    public void setIdMecanico(Integer idMecanico) { this.idMecanico = idMecanico; }
    public Long getIdVehiculo() { return idVehiculo; }
    public void setIdVehiculo(Long idVehiculo) { this.idVehiculo = idVehiculo; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
}