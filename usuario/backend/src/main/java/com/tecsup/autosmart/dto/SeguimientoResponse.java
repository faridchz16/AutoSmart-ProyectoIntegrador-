package com.tecsup.autosmart.dto;

import java.util.List;

public class SeguimientoResponse {

    private Long idCita;
    private String ticket;
    private String estado;
    private String servicio;
    private String placa;
    private String mecanico;
    private String fecha;
    private String horaInicio;

    private int porcentajeAvance;
    private List<Hito> lineaDeTiempo;

    public Long getIdCita() { return idCita; }
    public void setIdCita(Long idCita) { this.idCita = idCita; }
    public String getTicket() { return ticket; }
    public void setTicket(String ticket) { this.ticket = ticket; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getServicio() { return servicio; }
    public void setServicio(String servicio) { this.servicio = servicio; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getMecanico() { return mecanico; }
    public void setMecanico(String mecanico) { this.mecanico = mecanico; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }
    public int getPorcentajeAvance() { return porcentajeAvance; }
    public void setPorcentajeAvance(int porcentajeAvance) { this.porcentajeAvance = porcentajeAvance; }
    public List<Hito> getLineaDeTiempo() { return lineaDeTiempo; }
    public void setLineaDeTiempo(List<Hito> lineaDeTiempo) { this.lineaDeTiempo = lineaDeTiempo; }

    public static class Hito {
        private String clave;
        private String titulo;
        private boolean completado;
        private String fecha;
        private String detalle;

        public Hito(String clave, String titulo, boolean completado, String fecha, String detalle) {
            this.clave = clave;
            this.titulo = titulo;
            this.completado = completado;
            this.fecha = fecha;
            this.detalle = detalle;
        }

        public String getClave() { return clave; }
        public String getTitulo() { return titulo; }
        public boolean isCompletado() { return completado; }
        public String getFecha() { return fecha; }
        public String getDetalle() { return detalle; }
    }
}