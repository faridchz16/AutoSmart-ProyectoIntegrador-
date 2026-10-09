package com.tecsup.autosmart.data.model

data class LoginRequest(
    val correo: String,
    val password: String
)

data class RegisterRequest(
    val nombre: String,
    val correo: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val type: String,
    val correo: String,
    val rol: String
)

