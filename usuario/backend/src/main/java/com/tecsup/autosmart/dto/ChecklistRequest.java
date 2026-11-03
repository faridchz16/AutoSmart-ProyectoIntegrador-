package com.tecsup.autosmart.dto;

import com.tecsup.autosmart.model.ElementoChecklist;
import com.tecsup.autosmart.model.EstadoItemChecklist;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ChecklistRequest {

    @Size(max = 300, message = "La observacion general no puede superar 300 caracteres")
    private String observacionGeneral;

    @NotNull(message = "Los elementos del checklist son obligatorios")
    @Size(min = 5, max = 5, message = "El checklist debe tener los 5 elementos: LUCES, FRENOS, NEUMATICOS, FLUIDOS y CARROCERIA")
    @Valid
    private List<Item> items;

    public String getObservacionGeneral() { return observacionGeneral; }
    public void setObservacionGeneral(String observacionGeneral) { this.observacionGeneral = observacionGeneral; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {

        @NotNull(message = "El elemento es obligatorio")
        private ElementoChecklist elemento;

        @NotNull(message = "El estado es obligatorio (OK u OBSERVADO)")
        private EstadoItemChecklist estado;

        @Size(max = 300, message = "La observacion no puede superar 300 caracteres")
        private String observacion;

        public ElementoChecklist getElemento() { return elemento; }
        public void setElemento(ElementoChecklist elemento) { this.elemento = elemento; }
        public EstadoItemChecklist getEstado() { return estado; }
        public void setEstado(EstadoItemChecklist estado) { this.estado = estado; }
        public String getObservacion() { return observacion; }
        public void setObservacion(String observacion) { this.observacion = observacion; }
    }
}