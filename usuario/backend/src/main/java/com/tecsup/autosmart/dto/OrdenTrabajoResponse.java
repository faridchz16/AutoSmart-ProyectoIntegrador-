package com.tecsup.autosmart.dto;

public class OrdenTrabajoResponse {

    private Long idOrden;
    private Long idCita;
    private String ticket;
    private String estado;
    private String fechaInicio;
    private String servicio;
    private String placa;

    public Long getIdOrden() { return idOrden; }
    public void setIdOrden(Long idOrden) { this.idOrden = idOrden; }
    public Long getIdCita() { return idCita; }
    public void setIdCita(Long idCita) { this.idCita = idCita; }
    public String getTicket() { return ticket; }
    public void setTicket(String ticket) { this.ticket = ticket; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getServicio() { return servicio; }
    public void setServicio(String servicio) { this.servicio = servicio; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
}