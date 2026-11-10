package com.tecsup.autosmart.dto;

import java.math.BigDecimal;
import java.util.List;

public class HistorialVehiculoResponse {

    private Long idVehiculo;
    private String placa;
    private String marca;
    private String modelo;
    private Integer anio;
    private Integer kilometraje;
    private int totalMantenimientos;
    private List<Mantenimiento> mantenimientos;

    public Long getIdVehiculo() { return idVehiculo; }
    public void setIdVehiculo(Long idVehiculo) { this.idVehiculo = idVehiculo; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer kilometraje) { this.kilometraje = kilometraje; }
    public int getTotalMantenimientos() { return totalMantenimientos; }
    public void setTotalMantenimientos(int totalMantenimientos) { this.totalMantenimientos = totalMantenimientos; }
    public List<Mantenimiento> getMantenimientos() { return mantenimientos; }
    public void setMantenimientos(List<Mantenimiento> mantenimientos) { this.mantenimientos = mantenimientos; }

    public static class Mantenimiento {
        private Long idOrden;
        private Long idCita;
        private String ticket;
        private String estado;
        private String servicio;
        private String mecanico;
        private String fechaInicio;
        private String fechaFin;
        private String diagnostico;
        private BigDecimal costoRepuestos;

        public Mantenimiento(Long idOrden, Long idCita, String ticket, String estado, String servicio,
                             String mecanico, String fechaInicio, String fechaFin, String diagnostico,
                             BigDecimal costoRepuestos) {
            this.idOrden = idOrden;
            this.idCita = idCita;
            this.ticket = ticket;
            this.estado = estado;
            this.servicio = servicio;
            this.mecanico = mecanico;
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
            this.diagnostico = diagnostico;
            this.costoRepuestos = costoRepuestos;
        }

        public Long getIdOrden() { return idOrden; }
        public Long getIdCita() { return idCita; }
        public String getTicket() { return ticket; }
        public String getEstado() { return estado; }
        public String getServicio() { return servicio; }
        public String getMecanico() { return mecanico; }
        public String getFechaInicio() { return fechaInicio; }
        public String getFechaFin() { return fechaFin; }
        public String getDiagnostico() { return diagnostico; }
        public BigDecimal getCostoRepuestos() { return costoRepuestos; }
    }
}