package com.tecsup.autosmart.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime

data class ReprogramarRequest(val fecha: String, val horaInicio: String)

@RequiresApi(Build.VERSION_CODES.O)
fun esReprogramable(cita: CitaCreadaResponse, ahora: LocalDateTime): Boolean {
    if (cita.estado != "PENDIENTE" && cita.estado != "CONFIRMADA") return false
    return runCatching {
        val inicio = LocalDateTime.of(LocalDate.parse(cita.fecha), LocalTime.parse(cita.horaInicio))
        !ahora.plusHours(24).isAfter(inicio)
    }.getOrDefault(false)
}

@RequiresApi(Build.VERSION_CODES.O)
fun coincideHorario(cita: CitaCreadaResponse, datos: ReprogramarRequest): Boolean = runCatching {
    LocalDate.parse(cita.fecha) == LocalDate.parse(datos.fecha) &&
        LocalTime.parse(cita.horaInicio) == LocalTime.parse(datos.horaInicio)
}.getOrDefault(false)

@RequiresApi(Build.VERSION_CODES.O)
fun validarReprogramacion(cita: CitaCreadaResponse, datos: ReprogramarRequest, ahora: LocalDateTime): String? {
    if (!esReprogramable(cita, ahora)) return "Solo puedes reprogramar citas pendientes o confirmadas con al menos 24 horas de anticipación."
    val fecha = runCatching { LocalDate.parse(datos.fecha) }.getOrNull()
        ?: return "Selecciona una fecha válida."
    val hora = runCatching { LocalTime.parse(datos.horaInicio) }.getOrNull()
        ?: return "Selecciona una hora válida."
    if (!LocalDateTime.of(fecha, hora).isAfter(ahora)) return "El nuevo horario debe ser futuro."
    if (coincideHorario(cita, datos)) return "Selecciona una fecha u hora diferente de la actual."
    return null
}
