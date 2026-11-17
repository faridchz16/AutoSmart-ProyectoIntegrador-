package com.tecsup.autosmart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CostoOrdenResponse(
        Long idOrden,
        Long idCita,
        String ticket,
        String estadoCita,
        ServicioCobrado servicio,
        List<Adicional> adicionales,
        BigDecimal totalAdicionales,
        List<RepuestoUtilizado> repuestosUtilizados,
        BigDecimal total,
        String moneda,
        boolean puedePagar,
        PagoRealizado pago) {

    public record ServicioCobrado(String nombre, BigDecimal precio) {}

    public record Adicional(String tipo, String descripcion, Integer cantidad,
                            BigDecimal precioUnitario, BigDecimal subtotal) {}

    public record RepuestoUtilizado(String material, Integer cantidad, String unidad) {}

    public record PagoRealizado(Long idPago, String codigo, String metodo, String fecha) {}
}