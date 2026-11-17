package com.tecsup.autosmart.mapper;

import com.tecsup.autosmart.dto.CostoOrdenResponse.Adicional;
import com.tecsup.autosmart.dto.CostoOrdenResponse.RepuestoUtilizado;
import com.tecsup.autosmart.model.MovimientoInventario;
import com.tecsup.autosmart.model.PresupuestoItem;
import org.springframework.stereotype.Component;

@Component
public class CostoOrdenMapper {

    public Adicional toAdicional(PresupuestoItem item) {
        return new Adicional(item.getTipo().name(), item.getDescripcion(), item.getCantidad(),
                item.getPrecioUnitario(), item.getSubtotal());
    }

    public RepuestoUtilizado toRepuesto(MovimientoInventario salida) {
        return new RepuestoUtilizado(salida.getMaterial().getNombre(), salida.getCantidad(),
                salida.getMaterial().getUnidad());
    }
}