package com.tecsup.autosmart.dto;

import java.math.BigDecimal;
import java.util.List;

public class PresupuestoResponse {

    private Long idPresupuesto;
    private Long idOrden;
    private Long idCita;
    private String estado;
    private String descripcion;
    private String fechaCreacion;
    private String fechaDecision;
    private List<Item> items;
    private BigDecimal total;
    private List<DeficienciaResponse> deficiencias;

    public Long getIdPresupuesto() { return idPresupuesto; }
    public void setIdPresupuesto(Long idPresupuesto) { this.idPresupuesto = idPresupuesto; }
    public Long getIdOrden() { return idOrden; }
    public void setIdOrden(Long idOrden) { this.idOrden = idOrden; }
    public Long getIdCita() { return idCita; }
    public void setIdCita(Long idCita) { this.idCita = idCita; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getFechaDecision() { return fechaDecision; }
    public void setFechaDecision(String fechaDecision) { this.fechaDecision = fechaDecision; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<DeficienciaResponse> getDeficiencias() { return deficiencias; }
    public void setDeficiencias(List<DeficienciaResponse> deficiencias) { this.deficiencias = deficiencias; }

    public static class Item {
        private String tipo;
        private String descripcion;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;

        public Item(String tipo, String descripcion, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.subtotal = subtotal;
        }

        public String getTipo() { return tipo; }
        public String getDescripcion() { return descripcion; }
        public Integer getCantidad() { return cantidad; }
        public BigDecimal getPrecioUnitario() { return precioUnitario; }
        public BigDecimal getSubtotal() { return subtotal; }
    }
}