
package com.tecsup.autosmart.data.model

import java.math.BigDecimal

data class CitaCreadaResponse(
    val idCita: Long,
    val ticket: String,
    val estado: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val precio: BigDecimal,
    val servicio: String,
    val mecanico: String,
    val placa: String
)
