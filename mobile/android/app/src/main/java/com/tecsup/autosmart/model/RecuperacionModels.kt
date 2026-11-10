package com.tecsup.autosmart.model

data class ForgotPasswordRequest(val correo: String)

data class ResetPasswordRequest(
    val correo: String,
    val codigo: String,
    val nuevaPassword: String
)
