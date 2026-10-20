package com.tecsup.autosmart.data.model

data class MecanicoDto(
    val idMecanico: Int,
    val nombre: String,
    val aniosExperiencia: Int?,
    val especialidades: List<String>?
)

data class TurnoDto(
    val hora: String,
    val disponible: Boolean?
)