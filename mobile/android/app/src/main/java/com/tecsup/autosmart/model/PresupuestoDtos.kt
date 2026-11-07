package com.tecsup.autosmart.model

import java.math.BigDecimal

data class PresupuestoItemDto(
    val tipo: String?, val descripcion: String?, val cantidad: Int?,
    val precioUnitario: BigDecimal?, val subtotal: BigDecimal?
)

data class PresupuestoResponse(
    val idPresupuesto: Long, val idOrden: Long, val idCita: Long,
    val estado: String?, val descripcion: String?, val fechaCreacion: String?, val fechaDecision: String?,
    val items: List<PresupuestoItemDto>?, val total: BigDecimal?, val deficiencias: List<DeficienciaResponse>?
)
