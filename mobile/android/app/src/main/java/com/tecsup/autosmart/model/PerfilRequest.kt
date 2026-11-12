package com.tecsup.autosmart.model

data class PerfilRequest(val nombre: String, val telefono: String)

internal fun validarPerfil(nombre: String, telefono: String): String? = when {
    nombre.trim().isEmpty() -> "El nombre es obligatorio."
    nombre.trim().length > 100 -> "El nombre no puede superar 100 caracteres."
    !Regex("^(\\+?[0-9]{7,15})?$").matches(telefono.trim()) -> "El teléfono debe tener entre 7 y 15 dígitos, con + opcional."
    else -> null
}
