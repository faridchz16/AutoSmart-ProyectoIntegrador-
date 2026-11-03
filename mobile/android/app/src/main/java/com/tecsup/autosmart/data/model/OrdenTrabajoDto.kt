package com.tecsup.autosmart.data.model

data class OrdenTrabajoDto(
    val idOrden: Long = 0,
    val idCita: Long = 0,
    val ticket: String? = null,
    val estado: String? = null,
    val fechaInicio: String? = null,
    val servicio: String? = null,
    val placa: String? = null
)
