package com.tecsup.autosmart.data.model

data class PerfilResponse(
    val idUsuario: Int,
    val nombre: String?,
    val correo: String?,
    val telefono: String?,
    val rol: String?
)
