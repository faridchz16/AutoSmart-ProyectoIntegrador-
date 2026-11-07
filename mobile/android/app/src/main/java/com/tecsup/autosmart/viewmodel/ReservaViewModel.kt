
package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.model.CitaCreadaResponse
import com.tecsup.autosmart.model.CitaRequest
import com.tecsup.autosmart.model.MecanicoDisponible
import com.tecsup.autosmart.model.Turno
import com.tecsup.autosmart.data.repository.CitaRepository
import com.tecsup.autosmart.data.repository.ReservaRepository
import com.tecsup.autosmart.data.repository.ReservaRepositoryFake
import com.tecsup.autosmart.data.repository.ReservaRepositoryRetrofit
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


private const val USAR_DATOS_DE_PRUEBA_TURNOS = false

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


data class ConfirmarCitaUiState(
    val cargando: Boolean = false,
    val cita: CitaCreadaResponse? = null,
    val error: String? = null
)

class ReservaViewModel(
    private val repoMecanicos: ReservaRepository = ReservaRepositoryRetrofit(),
    private val repoTurnos: ReservaRepository =
        if (USAR_DATOS_DE_PRUEBA_TURNOS) {
            ReservaRepositoryFake()
        } else {
            ReservaRepositoryRetrofit()
        },
    private val citaRepository: CitaRepository = CitaRepository()
) : ViewModel() {


    private val _mecanicos = MutableStateFlow(MecanicosUiState())
    val mecanicos: StateFlow<MecanicosUiState> = _mecanicos


    private val _turnos = MutableStateFlow(TurnosUiState())
    val turnos: StateFlow<TurnosUiState> = _turnos
    private var consultaTurnos: Job? = null


    private val _confirmacion = MutableStateFlow(ConfirmarCitaUiState())
    val confirmacion: StateFlow<ConfirmarCitaUiState> = _confirmacion

    fun cargarMecanicos(idServicio: Long = 1L) {
        viewModelScope.launch {
            _mecanicos.value = MecanicosUiState(cargando = true)

            repoMecanicos.mecanicosPorServicio(idServicio)
                .onSuccess {
                    _mecanicos.value = MecanicosUiState(lista = it)
                }
                .onFailure {
                    _mecanicos.value = MecanicosUiState(
                        error = it.message
                            ?: "No se pudo cargar la lista de mecánicos"
                    )
                }
        }
    }

    fun cargarTurnos(
        idServicio: Long = 1L,
        fecha: String,
        idMecanico: Int
    ) {
        consultaTurnos?.cancel()
        consultaTurnos = viewModelScope.launch {
            _turnos.value = TurnosUiState(cargando = true)

            val resultado = repoTurnos.turnos(idServicio, fecha, idMecanico)
            currentCoroutineContext().ensureActive()

            resultado.onSuccess {
                    _turnos.value = TurnosUiState(lista = it)
                }
                .onFailure {
                    _turnos.value = TurnosUiState(
                        error = it.message
                            ?: "No se pudieron cargar los turnos"
                    )
                }
        }
    }


    fun confirmarCita(request: CitaRequest) {
        if (_confirmacion.value.cargando) return

        viewModelScope.launch {
            _confirmacion.value = ConfirmarCitaUiState(cargando = true)

            citaRepository.crearCita(request)
                .onSuccess { cita ->
                    _confirmacion.value = ConfirmarCitaUiState(
                        cita = cita
                    )
                }
                .onFailure { error ->
                    _confirmacion.value = ConfirmarCitaUiState(
                        error = error.message
                            ?: "No se pudo confirmar la reserva"
                    )
                }
        }
    }

    fun limpiarConfirmacion() {
        _confirmacion.value = ConfirmarCitaUiState()
    }
}
