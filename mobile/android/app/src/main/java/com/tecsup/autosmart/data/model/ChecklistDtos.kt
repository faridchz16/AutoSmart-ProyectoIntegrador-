package com.tecsup.autosmart.data.model

val elementosChecklist = listOf("LUCES", "FRENOS", "NEUMATICOS", "FLUIDOS", "CARROCERIA")

data class ChecklistItemDto(
    val elemento: String,
    val estado: String? = null,
    val observacion: String? = null
)

data class ChecklistRequest(val observacionGeneral: String?, val items: List<ChecklistItemDto>)

data class ChecklistResponse(
    val idChecklist: Long,
    val idOrden: Long,
    val fechaRegistro: String?,
    val observacionGeneral: String?,
    val items: List<ChecklistItemDto>?
)

internal fun validarChecklist(request: ChecklistRequest): String? = when {
    (request.observacionGeneral?.length ?: 0) > 300 -> "La observación general admite hasta 300 caracteres."
    request.items.size != 5 || request.items.map { it.elemento }.toSet() != elementosChecklist.toSet() ->
        "Debes completar los cinco elementos del checklist."
    request.items.any { it.estado !in setOf("OK", "OBSERVADO") } -> "Selecciona el estado de cada elemento."
    request.items.any { (it.observacion?.length ?: 0) > 300 } -> "Cada observación admite hasta 300 caracteres."
    request.items.any { it.estado == "OBSERVADO" && it.observacion.isNullOrBlank() } ->
        "Describe la observación de cada elemento marcado como Observado."
    else -> null
}
