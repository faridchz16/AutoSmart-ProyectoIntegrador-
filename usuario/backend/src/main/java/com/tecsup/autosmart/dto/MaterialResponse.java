package com.tecsup.autosmart.dto;

import java.math.BigDecimal;

public class MaterialResponse {

    private Long idMaterial;
    private String codigo;
    private String nombre;
    private String unidad;
    private BigDecimal costoUnitario;
    private Integer stockActual;
    private Integer stockMinimo;
    private boolean bajoStock;

    public Long getIdMaterial() { return idMaterial; }
    public void setIdMaterial(Long idMaterial) { this.idMaterial = idMaterial; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public Integer getStockActual() { return stockActual; }
    public void setStockActual(Integer stockActual) { this.stockActual = stockActual; }
    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }
    public boolean isBajoStock() { return bajoStock; }
    public void setBajoStock(boolean bajoStock) { this.bajoStock = bajoStock; }
}