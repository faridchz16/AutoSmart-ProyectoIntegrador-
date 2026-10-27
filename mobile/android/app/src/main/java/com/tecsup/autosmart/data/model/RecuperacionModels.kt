package com.tecsup.autosmart.data.model

data class ForgotPasswordRequest(val correo: String)

data class ResetPasswordRequest(
    val correo: String,
    val codigo: String,
    val nuevaPassword: String
)
