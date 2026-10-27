package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.repository.RecuperacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RecuperacionUiState(
    val enviando: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val codigoEnviado: Boolean = false,
    val contrasenaActualizada: Boolean = false
)

class RecuperacionViewModel : ViewModel() {

    private val repo = RecuperacionRepository()

    private val _estado = MutableStateFlow(RecuperacionUiState())
    val estado: StateFlow<RecuperacionUiState> = _estado

    fun solicitarCodigo(correo: String, reenvio: Boolean = false) {
        if (_estado.value.enviando) return
        _estado.value = RecuperacionUiState(enviando = true)
        viewModelScope.launch {
            repo.solicitarCodigo(correo)
                .onSuccess {
                    _estado.value = RecuperacionUiState(
                        codigoEnviado = !reenvio,
                        info = if (reenvio) "Te enviamos un nuevo código a tu correo" else null
                    )
                }
                .onFailure { _estado.value = RecuperacionUiState(error = it.message) }
        }
    }

    fun restablecer(correo: String, codigo: String, nuevaPassword: String) {
        if (_estado.value.enviando) return
        _estado.value = RecuperacionUiState(enviando = true)
        viewModelScope.launch {
            repo.restablecer(correo, codigo, nuevaPassword)
                .onSuccess { _estado.value = RecuperacionUiState(contrasenaActualizada = true) }
                .onFailure { _estado.value = RecuperacionUiState(error = it.message) }
        }
    }

    fun limpiarMensajes() {
        _estado.value = _estado.value.copy(error = null, info = null)
    }

    fun consumirCodigoEnviado() {
        _estado.value = _estado.value.copy(codigoEnviado = false)
    }

    fun consumirActualizada() {
        _estado.value = _estado.value.copy(contrasenaActualizada = false)
    }

    fun reiniciar() {
        _estado.value = RecuperacionUiState()
    }
}
