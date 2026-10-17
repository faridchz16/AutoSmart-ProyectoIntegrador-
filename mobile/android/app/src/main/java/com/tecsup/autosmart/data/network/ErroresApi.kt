package com.tecsup.autosmart.data.network

import org.json.JSONObject

fun mensajeDeError(cuerpo: String?, codigo: Int): String {
    val texto = try {
        cuerpo?.let {
            val json = JSONObject(it)
            json.optString("mensaje").ifEmpty { json.optString("message") }.ifEmpty { json.optString("error") }
        }
    } catch (e: Exception) {
        null
    }
    return if (!texto.isNullOrBlank()) texto else when (codigo) {
        401 -> "Tu sesión expiró. Vuelve a iniciar sesión"
        403 -> "No tienes permisos para esta acción"
        404 -> "No se encontró lo solicitado"
        else -> "Error del servidor ($codigo)"
    }
}