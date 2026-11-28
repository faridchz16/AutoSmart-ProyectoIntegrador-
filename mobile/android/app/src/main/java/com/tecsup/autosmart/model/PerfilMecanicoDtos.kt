package com.tecsup.autosmart.model

/** GET/PUT /mechanics/me: perfil profesional del mecánico (HU-15/T15.x del backend). */
data class PerfilMecanicoDto(
    val idUsuario: Int,
    val nombre: String?,
    val correo: String?,
    val aniosExperiencia: Int?,
    val especialidades: List<String>?
)

data class PerfilMecanicoRequest(
    val aniosExperiencia: Int,
    val especialidades: List<String>
)
