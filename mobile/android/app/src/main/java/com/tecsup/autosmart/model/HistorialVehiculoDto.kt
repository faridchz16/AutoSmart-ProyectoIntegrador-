package com.tecsup.autosmart.model

import java.math.BigDecimal

data class HistorialVehiculoDto(
    val idVehiculo: Long,
    val mantenimientos: List<MantenimientoHistorialDto>?
)

data class MantenimientoHistorialDto(
    val idOrden: Long,
    val ticket: String?,
    val servicio: String?,
    val mecanico: String?,
    val fechaFin: String?,
    val costoRepuestos: BigDecimal?,
    // Sprint 4: el backend ya los envía; sirven para saber si se puede pagar
    val idCita: Long? = null,
    val estado: String? = null
)
