package com.tecsup.autosmart.dto;

import com.tecsup.autosmart.model.TipoItemPresupuesto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class PresupuestoRequest {

    @NotBlank(message = "La descripcion del presupuesto es obligatoria")
    @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
    private String descripcion;

    @NotEmpty(message = "El presupuesto debe tener al menos un item")
    @Size(max = 30, message = "El presupuesto no puede tener mas de 30 items")
    @Valid
    private List<Item> items;

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {

        @NotNull(message = "El tipo es obligatorio (MANO_OBRA o REPUESTO)")
        private TipoItemPresupuesto tipo;

        @NotBlank(message = "La descripcion del item es obligatoria")
        @Size(max = 200, message = "La descripcion del item no puede superar 200 caracteres")
        private String descripcion;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer cantidad;

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo")
        private BigDecimal precioUnitario;

        public TipoItemPresupuesto getTipo() { return tipo; }
        public void setTipo(TipoItemPresupuesto tipo) { this.tipo = tipo; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public BigDecimal getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    }
}