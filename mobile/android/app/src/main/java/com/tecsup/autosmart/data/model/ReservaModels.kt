package com.tecsup.autosmart.data.model

// Modelo UI para representar los datos de los mecánicos disponibles
data class MecanicoDisponible(
    val idMecanico: Int,
    val nombre: String,
    val aniosExperiencia: Int,
    val especialidades: List<String>
)

// Modelo UI para los turnos y horas disponibles en el calendario
data class Turno(
    val hora: String,
    val disponible: Boolean
)