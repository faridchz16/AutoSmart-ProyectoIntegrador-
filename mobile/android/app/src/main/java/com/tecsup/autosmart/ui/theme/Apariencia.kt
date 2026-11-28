package com.tecsup.autosmart.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Modo claro / oscuro elegido por el usuario en "Mi perfil".
 * Se guarda en el celular; si nunca eligió, sigue el modo del sistema (null).
 */
object Apariencia {
    private const val ARCHIVO = "apariencia"
    private const val CLAVE = "modo_oscuro"

    /** true = oscuro, false = claro, null = igual que el sistema. */
    var modoOscuro by mutableStateOf<Boolean?>(null)
        private set

    fun iniciar(context: Context) {
        val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        modoOscuro = if (prefs.contains(CLAVE)) prefs.getBoolean(CLAVE, false) else null
    }

    fun cambiar(context: Context, oscuro: Boolean) {
        modoOscuro = oscuro
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .edit().putBoolean(CLAVE, oscuro).apply()
    }
}
