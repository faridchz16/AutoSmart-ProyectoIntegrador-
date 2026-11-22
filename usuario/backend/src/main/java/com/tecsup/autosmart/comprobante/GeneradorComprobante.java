package com.tecsup.autosmart.comprobante;

import com.tecsup.autosmart.dto.CostoOrdenResponse;
import com.tecsup.autosmart.model.Pago;

public interface GeneradorComprobante {

    byte[] generar(Pago pago, CostoOrdenResponse costo);
}