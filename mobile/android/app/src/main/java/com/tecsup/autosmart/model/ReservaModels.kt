package com.tecsup.autosmart.model

// Modelo UI para representar los datos de los mecánicos disponibles
data class MecanicoDisponible(
    val idMecanico: Int,
    val nombre: String,
    val aniosExperiencia: Int,
    val especialidades: List<String>,
    // T66.1: promedio de estrellas (null si aún no tiene reseñas) y cantidad de reseñas
    val calificacionPromedio: Double? = null,
    val totalResenas: Long = 0
)

/** T66.1: primero los mejor calificados (y con más reseñas); los nuevos sin reseñas van al final. */
fun List<MecanicoDisponible>.ordenarPorCalificacion(): List<MecanicoDisponible> =
    sortedWith(
        compareByDescending<MecanicoDisponible> { it.calificacionPromedio ?: -1.0 }
            .thenByDescending { it.totalResenas }
            .thenBy { it.nombre.lowercase() }
    )

// Modelo UI para los turnos y horas disponibles en el calendario
data class Turno(
    val hora: String,
    val disponible: Boolean
)