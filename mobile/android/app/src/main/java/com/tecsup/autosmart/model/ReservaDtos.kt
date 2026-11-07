package com.tecsup.autosmart.model

import com.google.gson.annotations.SerializedName

data class MecanicoDto(
    val idMecanico: Int,
    val nombre: String,
    val aniosExperiencia: Int?,
    val especialidades: List<String>?
)

data class TurnoDto(
    @SerializedName("horaInicio")
    val hora: String,
    val disponible: Boolean?
)