package com.tecsup.autosmart.chatbot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class BorradorCita {

    private PasoAgendar paso = PasoAgendar.SERVICIO;
    private Long idServicio;
    private String servicio;
    private BigDecimal precio;
    private Long idVehiculo;
    private String placa;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private Integer idMecanico;
    private String mecanico;

    public PasoAgendar getPaso() { return paso; }
    public void setPaso(PasoAgendar paso) { this.paso = paso; }
    public Long getIdServicio() { return idServicio; }
    public String getServicio() { return servicio; }
    public BigDecimal getPrecio() { return precio; }
    public Long getIdVehiculo() { return idVehiculo; }
    public String getPlaca() { return placa; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public Integer getIdMecanico() { return idMecanico; }
    public String getMecanico() { return mecanico; }

    public void elegirServicio(Long idServicio, String servicio, BigDecimal precio) {
        this.idServicio = idServicio;
        this.servicio = servicio;
        this.precio = precio;
    }

    public void elegirVehiculo(Long idVehiculo, String placa) {
        this.idVehiculo = idVehiculo;
        this.placa = placa;
    }

    public void elegirTurno(LocalTime horaInicio, Integer idMecanico, String mecanico) {
        this.horaInicio = horaInicio;
        this.idMecanico = idMecanico;
        this.mecanico = mecanico;
    }

    public void limpiarTurno() {
        elegirTurno(null, null, null);
    }
}