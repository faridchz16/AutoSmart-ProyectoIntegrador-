
package com.tecsup.autosmart.data.model

data class CitaRequest(
    val idServicio: Long,
    val idMecanico: Int,
    val idVehiculo: Long,
    val fecha: String,
    val horaInicio: String
)
