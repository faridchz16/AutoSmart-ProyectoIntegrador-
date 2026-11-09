package com.tecsup.autosmart.model

data class SeguimientoDto(
    val idCita: Long,
    val ticket: String?,
    val estado: String?,
    val servicio: String?,
    val placa: String?,
    val mecanico: String?,
    val fecha: String?,
    val horaInicio: String?,
    val porcentajeAvance: Int?,
    val lineaDeTiempo: List<HitoSeguimiento?>?
)

data class HitoSeguimiento(
    val clave: String?,
    val titulo: String?,
    val completado: Boolean?,
    val fecha: String?,
    val detalle: String?
)
