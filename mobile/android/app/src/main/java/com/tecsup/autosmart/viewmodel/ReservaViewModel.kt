
package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.model.CitaCreadaResponse
import com.tecsup.autosmart.model.CitaRequest
import com.tecsup.autosmart.model.MecanicoDisponible
import com.tecsup.autosmart.model.Turno
import com.tecsup.autosmart.data.repository.BloqueoTurnoRepository
import com.tecsup.autosmart.data.repository.CitaRepository
import com.tecsup.autosmart.model.BloqueoRequest
import com.tecsup.autosmart.model.BloqueoResponse
import com.tecsup.autosmart.data.repository.ReservaRepository
import com.tecsup.autosmart.data.repository.ReservaRepositoryFake
import com.tecsup.autosmart.data.repository.ReservaRepositoryRetrofit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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


// T33.2: estado del bloqueo real del turno en el servidor
data class BloqueoUiState(
    val cargando: Boolean = false,
    val bloqueo: BloqueoResponse? = null,
    val hora: String? = null,
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
    private val citaRepository: CitaRepository = CitaRepository(),
    private val bloqueoRepository: BloqueoTurnoRepository = BloqueoTurnoRepository(),
    // La liberación usa su propio scope para que el DELETE se envíe aunque se cierre la pantalla
    private val scopeLiberacion: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ViewModel() {


    private val _mecanicos = MutableStateFlow(MecanicosUiState())
    val mecanicos: StateFlow<MecanicosUiState> = _mecanicos


    private val _turnos = MutableStateFlow(TurnosUiState())
    val turnos: StateFlow<TurnosUiState> = _turnos
    private var consultaTurnos: Job? = null


    private val _confirmacion = MutableStateFlow(ConfirmarCitaUiState())
    val confirmacion: StateFlow<ConfirmarCitaUiState> = _confirmacion

    private val _bloqueo = MutableStateFlow(BloqueoUiState())
    val bloqueo: StateFlow<BloqueoUiState> = _bloqueo
    private var operacionBloqueo: Job? = null

    // T33.2: al elegir un turno se bloquea en el servidor (POST /appointments/holds).
    // El backend deja un solo bloqueo por cliente, así que el anterior se reemplaza.
    fun bloquearTurno(idServicio: Long, idMecanico: Int, fecha: String, hora: String) {
        if (_confirmacion.value.cargando) return
        operacionBloqueo?.cancel()
        val anterior = _bloqueo.value.bloqueo
        _bloqueo.value = BloqueoUiState(cargando = true, hora = hora)
        operacionBloqueo = viewModelScope.launch {
            val resultado = try {
                bloqueoRepository.bloquear(BloqueoRequest(idServicio, idMecanico, fecha, hora))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            currentCoroutineContext().ensureActive()
            resultado.onSuccess {
                _bloqueo.value = BloqueoUiState(bloqueo = it, hora = hora)
            }.onFailure {
                // si el nuevo bloqueo falla, el anterior ya no sirve: se libera
                anterior?.let { previo -> liberarEnServidor(previo.idBloqueo) }
                _bloqueo.value = BloqueoUiState(
                    error = it.message ?: "El turno ya no está disponible. Elige otro."
                )
            }
        }
    }

    // T33.2: libera el turno (DELETE /appointments/holds/{id}) al cambiar de mecánico,
    // de fecha, al vencer el tiempo o al salir de la reserva.
    fun liberarBloqueo() {
        operacionBloqueo?.cancel()
        operacionBloqueo = null
        _bloqueo.value.bloqueo?.let { liberarEnServidor(it.idBloqueo) }
        _bloqueo.value = BloqueoUiState()
    }

    fun limpiarErrorBloqueo() {
        if (_bloqueo.value.error != null) _bloqueo.value = _bloqueo.value.copy(error = null)
    }

    private fun liberarEnServidor(idBloqueo: Long) {
        scopeLiberacion.launch { bloqueoRepository.liberar(idBloqueo) }
    }

    override fun onCleared() {
        // si el usuario sale sin confirmar, se libera el turno
        if (_confirmacion.value.cita == null) {
            _bloqueo.value.bloqueo?.let { bloqueo ->
                scopeLiberacion.launch {
                    bloqueoRepository.liberar(bloqueo.idBloqueo)
                    scopeLiberacion.cancel()
                }
            } ?: scopeLiberacion.cancel()
        } else {
            scopeLiberacion.cancel()
        }
        super.onCleared()
    }

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
                    // el backend ya libera el bloqueo del cliente al registrar la cita (T33.1)
                    operacionBloqueo?.cancel()
                    _bloqueo.value = BloqueoUiState()
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
