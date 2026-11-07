package com.tecsup.autosmart.dto;

import java.math.BigDecimal;

public class FinalizacionResponse {

    private Long idOrden;
    private Long idCita;
    private String ticket;
    private String estado;
    private String fechaInicio;
    private String fechaFin;
    private BigDecimal costoRepuestos;

    public FinalizacionResponse(Long idOrden, Long idCita, String ticket, String estado,
                                String fechaInicio, String fechaFin, BigDecimal costoRepuestos) {
        this.idOrden = idOrden;
        this.idCita = idCita;
        this.ticket = ticket;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.costoRepuestos = costoRepuestos;
    }

    public Long getIdOrden() { return idOrden; }
    public Long getIdCita() { return idCita; }
    public String getTicket() { return ticket; }
    public String getEstado() { return estado; }
    public String getFechaInicio() { return fechaInicio; }
    public String getFechaFin() { return fechaFin; }
    public BigDecimal getCostoRepuestos() { return costoRepuestos; }
}