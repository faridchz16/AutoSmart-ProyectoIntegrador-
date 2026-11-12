package com.tecsup.autosmart.model

// T33.2: contrato de POST /appointments/holds (BloqueoRequest / BloqueoResponse del backend)
data class BloqueoRequest(
    val idServicio: Long,
    val idMecanico: Int,
    val fecha: String,      // yyyy-MM-dd
    val horaInicio: String  // HH:mm
)

data class BloqueoResponse(
    val idBloqueo: Long,
    val fecha: String?,
    val horaInicio: String?,
    val horaFin: String?,
    val expiraEn: String?,
    val segundosRestantes: Long
)
