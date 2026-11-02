package com.tecsup.autosmart.data.model

import java.math.BigDecimal

// Contratos de citas del Sprint 02. idCita no identifica una orden de trabajo.
data class AgendaMecanicoDto(
    val idCita: Long,
    val ticket: String?,
    val fecha: String?,
    val horaInicio: String?,
    val horaFin: String?,
    val estado: String?,
    val servicio: String?,
    val cliente: String?,
    val placa: String?
)

data class CitaDetalleMecanicoDto(
    val idCita: Long,
    val ticket: String?,
    val fecha: String?,
    val horaInicio: String?,
    val horaFin: String?,
    val estado: String?,
    val precio: BigDecimal?,
    val servicio: String?,
    val duracionMinutos: Int?,
    val cliente: String?,
    val telefonoCliente: String?,
    val placa: String?,
    val marca: String?,
    val modelo: String?,
    val anio: Int?,
    val kilometraje: Int?
)
