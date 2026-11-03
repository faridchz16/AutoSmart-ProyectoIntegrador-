package com.tecsup.autosmart.dto;

import java.util.List;

public class ChecklistResponse {

    private Long idChecklist;
    private Long idOrden;
    private String fechaRegistro;
    private String observacionGeneral;
    private List<Item> items;

    public Long getIdChecklist() { return idChecklist; }
    public void setIdChecklist(Long idChecklist) { this.idChecklist = idChecklist; }
    public Long getIdOrden() { return idOrden; }
    public void setIdOrden(Long idOrden) { this.idOrden = idOrden; }
    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getObservacionGeneral() { return observacionGeneral; }
    public void setObservacionGeneral(String observacionGeneral) { this.observacionGeneral = observacionGeneral; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {
        private String elemento;
        private String estado;
        private String observacion;

        public Item(String elemento, String estado, String observacion) {
            this.elemento = elemento;
            this.estado = estado;
            this.observacion = observacion;
        }

        public String getElemento() { return elemento; }
        public String getEstado() { return estado; }
        public String getObservacion() { return observacion; }
    }
}