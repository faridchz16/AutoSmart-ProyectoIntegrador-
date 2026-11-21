package com.tecsup.autosmart.model

import java.math.BigDecimal

// Sprint 4 · HU-61/62/63: contratos de la API de pagos (feature/backend-payments de Farid)

/** GET /work-orders/{id}/cost (T61.1) */
data class CostoOrdenDto(
    val idOrden: Long,
    val idCita: Long?,
    val ticket: String?,
    val estadoCita: String?,
    val servicio: ServicioCobradoDto?,
    val adicionales: List<AdicionalCostoDto>?,
    val totalAdicionales: BigDecimal?,
    val repuestosUtilizados: List<RepuestoUtilizadoDto>?,
    val total: BigDecimal?,
    val moneda: String?,
    val puedePagar: Boolean,
    val pago: PagoRealizadoDto?
)

data class ServicioCobradoDto(val nombre: String?, val precio: BigDecimal?)

data class AdicionalCostoDto(
    val tipo: String?,
    val descripcion: String?,
    val cantidad: Int?,
    val precioUnitario: BigDecimal?,
    val subtotal: BigDecimal?
)

data class RepuestoUtilizadoDto(val material: String?, val cantidad: Int?, val unidad: String?)

data class PagoRealizadoDto(val idPago: Long, val codigo: String?, val metodo: String?, val fecha: String?)

/** GET /payment-methods (T62.1) */
data class MetodoPagoDto(val codigo: String, val nombre: String?, val enLinea: Boolean)

/** POST /payments (T63.1) */
data class PagoRequest(val idOrden: Long, val metodo: String, val tokenPago: String)

data class PagoDto(
    val idPago: Long,
    val codigo: String?,
    val idOrden: Long,
    val ticket: String?,
    val metodo: String?,
    val estado: String?,
    val monto: BigDecimal?,
    val fecha: String?,
    val motivoRechazo: String?,
    val comprobanteUrl: String?,
    val registradoPor: String?
)
