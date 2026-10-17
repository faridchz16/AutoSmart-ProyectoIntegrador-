package com.tecsup.autosmart.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.model.MecanicoDisponible
import com.tecsup.autosmart.data.model.Turno
import com.tecsup.autosmart.data.repository.ReservaRepository
import com.tecsup.autosmart.data.repository.ReservaRepositoryFake
import com.tecsup.autosmart.data.repository.ReservaRepositoryRetrofit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Cambiar a 'false' cuando el backend esté integrado
private const val USAR_DATOS_DE_PRUEBA = true

data class MecanicosUiState(
    val cargando: Boolean = false,
    val lista: List<MecanicoDisponible> = emptyList(),
    val error: String? = null
)

data class TurnosUiState(
    val cargando: Boolean = false,
    val lista: List<Turno> = emptyList(),
    val error: String? = null
)

class ReservaViewModel(
    private val repo: ReservaRepository =
        if (USAR_DATOS_DE_PRUEBA) ReservaRepositoryFake() else ReservaRepositoryRetrofit()
) : ViewModel() {

    private val _mecanicos = MutableStateFlow(MecanicosUiState())
    val mecanicos: StateFlow<MecanicosUiState> = _mecanicos

    private val _turnos = MutableStateFlow(TurnosUiState())
    val turnos: StateFlow<TurnosUiState> = _turnos

    fun cargarMecanicos(idServicio: Long = 1L) {
        viewModelScope.launch {
            _mecanicos.value = MecanicosUiState(cargando = true)
            repo.mecanicosPorServicio(idServicio)
                .onSuccess { _mecanicos.value = MecanicosUiState(lista = it) }
                .onFailure { _mecanicos.value = MecanicosUiState(error = it.message ?: "No se pudo cargar la lista de mecánicos") }
        }
    }

    fun cargarTurnos(idServicio: Long = 1L, fecha: String, idMecanico: Int) {
        viewModelScope.launch {
            _turnos.value = TurnosUiState(cargando = true)
            repo.turnos(idServicio, fecha, idMecanico)
                .onSuccess { _turnos.value = TurnosUiState(lista = it) }
                .onFailure { _turnos.value = TurnosUiState(error = it.message ?: "No se pudieron cargar los turnos") }
        }
    }
}