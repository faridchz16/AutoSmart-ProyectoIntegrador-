package com.tecsup.autosmart.model

val categoriasDeficiencia = listOf("MOTOR", "FRENOS", "SUSPENSION", "ELECTRICO", "NEUMATICOS", "CARROCERIA", "FLUIDOS", "OTRO")
val severidadesDeficiencia = listOf("LEVE", "MODERADA", "CRITICA")

data class DeficienciaRequest(val categoria: String, val severidad: String, val descripcion: String)
data class DeficienciaResponse(
    val idDeficiencia: Long,
    val idOrden: Long,
    val categoria: String?,
    val severidad: String?,
    val descripcion: String?,
    val fechaRegistro: String?,
    val fotos: List<FotoReferenciaDto>? = emptyList()
)

internal fun validarDeficiencia(request: DeficienciaRequest): String? = when {
    request.categoria !in categoriasDeficiencia -> "Selecciona una categoría."
    request.severidad !in severidadesDeficiencia -> "Selecciona la severidad."
    request.descripcion.isBlank() -> "Describe la deficiencia detectada."
    request.descripcion.length > 500 -> "La descripción admite hasta 500 caracteres."
    else -> null
}
