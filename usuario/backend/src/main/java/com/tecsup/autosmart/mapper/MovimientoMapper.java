package com.tecsup.autosmart.mapper;

import com.tecsup.autosmart.dto.MovimientoResponse;
import com.tecsup.autosmart.model.MovimientoInventario;
import org.springframework.stereotype.Component;

@Component
public class MovimientoMapper {

    public MovimientoResponse toResponse(MovimientoInventario m) {
        MovimientoResponse res = new MovimientoResponse();
        res.setIdMovimiento(m.getIdMovimiento());
        res.setIdMaterial(m.getMaterial().getIdMaterial());
        res.setCodigoMaterial(m.getMaterial().getCodigo());
        res.setTipo(m.getTipo().name());
        res.setCantidad(m.getCantidad());
        res.setCostoUnitario(m.getCostoUnitario());
        res.setStockResultante(m.getStockResultante());
        res.setReferencia(m.getReferencia());
        res.setIdOrden(m.getOrden() == null ? null : m.getOrden().getIdOrden());
        res.setRegistradoPor(m.getRegistradoPor());
        res.setFecha(m.getFecha().toString());
        return res;
    }
}