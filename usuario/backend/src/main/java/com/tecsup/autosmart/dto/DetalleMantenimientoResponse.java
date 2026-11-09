package com.tecsup.autosmart.dto;

import java.math.BigDecimal;
import java.util.List;

public class DetalleMantenimientoResponse {

    private Long idOrden;
    private Long idCita;
    private String ticket;
    private String estado;
    private String servicio;
    private BigDecimal precioServicio;
    private String placa;
    private String vehiculo;
    private String mecanico;
    private String fechaInicio;
    private String fechaFin;
    private String diagnostico;
    private List<ItemChecklist> checklist;
    private List<DeficienciaResponse> deficiencias;
    private TareasOrdenResponse tareas;
    private ResumenPresupuesto presupuesto;
    private List<MovimientoResponse> repuestosConsumidos;
    private BigDecimal costoRepuestos;
    // Precio del servicio + presupuesto aprobado (los repuestos consumidos ya estan dentro del presupuesto)
    private BigDecimal totalEstimado;

    public Long getIdOrden() { return idOrden; }
    public void setIdOrden(Long idOrden) { this.idOrden = idOrden; }
    public Long getIdCita() { return idCita; }
    public void setIdCita(Long idCita) { this.idCita = idCita; }
    public String getTicket() { return ticket; }
    public void setTicket(String ticket) { this.ticket = ticket; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getServicio() { return servicio; }
    public void setServicio(String servicio) { this.servicio = servicio; }
    public BigDecimal getPrecioServicio() { return precioServicio; }
    public void setPrecioServicio(BigDecimal precioServicio) { this.precioServicio = precioServicio; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getVehiculo() { return vehiculo; }
    public void setVehiculo(String vehiculo) { this.vehiculo = vehiculo; }
    public String getMecanico() { return mecanico; }
    public void setMecanico(String mecanico) { this.mecanico = mecanico; }
    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public List<ItemChecklist> getChecklist() { return checklist; }
    public void setChecklist(List<ItemChecklist> checklist) { this.checklist = checklist; }
    public List<DeficienciaResponse> getDeficiencias() { return deficiencias; }
    public void setDeficiencias(List<DeficienciaResponse> deficiencias) { this.deficiencias = deficiencias; }
    public TareasOrdenResponse getTareas() { return tareas; }
    public void setTareas(TareasOrdenResponse tareas) { this.tareas = tareas; }
    public ResumenPresupuesto getPresupuesto() { return presupuesto; }
    public void setPresupuesto(ResumenPresupuesto presupuesto) { this.presupuesto = presupuesto; }
    public List<MovimientoResponse> getRepuestosConsumidos() { return repuestosConsumidos; }
    public void setRepuestosConsumidos(List<MovimientoResponse> repuestosConsumidos) { this.repuestosConsumidos = repuestosConsumidos; }
    public BigDecimal getCostoRepuestos() { return costoRepuestos; }
    public void setCostoRepuestos(BigDecimal costoRepuestos) { this.costoRepuestos = costoRepuestos; }
    public BigDecimal getTotalEstimado() { return totalEstimado; }
    public void setTotalEstimado(BigDecimal totalEstimado) { this.totalEstimado = totalEstimado; }

    public static class ItemChecklist {
        private String elemento;
        private String estado;
        private String observacion;

        public ItemChecklist(String elemento, String estado, String observacion) {
            this.elemento = elemento;
            this.estado = estado;
            this.observacion = observacion;
        }

        public String getElemento() { return elemento; }
        public String getEstado() { return estado; }
        public String getObservacion() { return observacion; }
    }

    public static class ResumenPresupuesto {
        private Long idPresupuesto;
        private String estado;
        private String descripcion;
        private BigDecimal total;
        private List<PresupuestoResponse.Item> items;

        public ResumenPresupuesto(Long idPresupuesto, String estado, String descripcion, BigDecimal total,
                                  List<PresupuestoResponse.Item> items) {
            this.idPresupuesto = idPresupuesto;
            this.estado = estado;
            this.descripcion = descripcion;
            this.total = total;
            this.items = items;
        }

        public Long getIdPresupuesto() { return idPresupuesto; }
        public String getEstado() { return estado; }
        public String getDescripcion() { return descripcion; }
        public BigDecimal getTotal() { return total; }
        public List<PresupuestoResponse.Item> getItems() { return items; }
    }
}