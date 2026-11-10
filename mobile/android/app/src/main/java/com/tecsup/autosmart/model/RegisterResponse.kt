package com.tecsup.autosmart.model

data class RegisterResponse(
    val idUsuario: Long,
    val correo: String,
    val mensaje: String,
    val nombre: String,
    val rol: String
)