package com.tecsup.autosmart.dto;

import java.math.BigDecimal;
import java.util.List;

public class ConsumosOrdenResponse {

    private Long idOrden;
    private BigDecimal costoRepuestos;
    private List<MovimientoResponse> consumos;

    public ConsumosOrdenResponse(Long idOrden, BigDecimal costoRepuestos, List<MovimientoResponse> consumos) {
        this.idOrden = idOrden;
        this.costoRepuestos = costoRepuestos;
        this.consumos = consumos;
    }

    public Long getIdOrden() { return idOrden; }
    public BigDecimal getCostoRepuestos() { return costoRepuestos; }
    public List<MovimientoResponse> getConsumos() { return consumos; }
}