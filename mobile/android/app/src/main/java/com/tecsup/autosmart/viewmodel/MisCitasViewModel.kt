package com.tecsup.autosmart.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId
import com.tecsup.autosmart.data.repository.CancelacionInciertaException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.model.CitaCreadaResponse
import com.tecsup.autosmart.data.repository.CitaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MisCitasUiState(
    val cargando: Boolean = true,
    val citas: List<CitaCreadaResponse> = emptyList(),
    val error: String? = null,
    val aviso: String? = null,
    val cancelacionesPorVerificar: Set<Long> = emptySet()
)

data class CancelacionUiState(
    val cita: CitaCreadaResponse? = null,
    val enviando: Boolean = false,
    val verificando: Boolean = false,
    val requiereVerificacion: Boolean = false,
    val error: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
class MisCitasViewModel(
    private val cancelar: suspend (Long) -> Result<CitaCreadaResponse> = {
        CitaRepository().cancelarCita(it)
    },
    private val ahora: () -> LocalDateTime = { LocalDateTime.now(ZoneId.of("America/Lima")) },
    private val consultar: suspend () -> Result<List<CitaCreadaResponse>> = {
        CitaRepository().misCitas()
    }
) : ViewModel() {
    private val _estado = MutableStateFlow(MisCitasUiState())
    val estado: StateFlow<MisCitasUiState> = _estado
    private var consulta: Job? = null
    private var iniciada = false
    private val _cancelacion = MutableStateFlow(CancelacionUiState())
    val cancelacion: StateFlow<CancelacionUiState> = _cancelacion
    private var operacionCancelacion: Job? = null

    fun cargarSiNecesario() {
        if (!iniciada) cargarCitas()
    }

    fun cargarCitas() {
        if (consulta?.isActive == true || operacionCancelacion?.isActive == true) return
        iniciada = true
        _estado.value = MisCitasUiState(cargando = true)
        consulta = viewModelScope.launch {
            try {
                val resultado = consultar()
                currentCoroutineContext().ensureActive()
                resultado.onSuccess {
                    _estado.value = MisCitasUiState(cargando = false, citas = it)
                }.onFailure {
                    _estado.value = MisCitasUiState(
                        cargando = false,
                        error = it.message ?: "No se pudieron cargar tus citas"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.value = MisCitasUiState(
                    cargando = false,
                    error = e.message ?: "No se pudieron cargar tus citas"
                )
            }
        }
    }

    fun puedeCancelar(idCita: Long): Boolean {
        val cita = _estado.value.citas.find { it.idCita == idCita } ?: return false
        return idCita !in _estado.value.cancelacionesPorVerificar &&
                esCancelable(cita, ahora())
    }

    fun abrirCancelacion(idCita: Long) {
        if (consulta?.isActive == true || operacionCancelacion?.isActive == true) return
        val cita = _estado.value.citas.find { it.idCita == idCita } ?: return
        if (!puedeCancelar(idCita)) return
        _cancelacion.value = CancelacionUiState(cita = cita)
    }

    fun cerrarCancelacion() {
        if (_cancelacion.value.enviando || _cancelacion.value.verificando) return
        _cancelacion.value = CancelacionUiState()
    }

    fun confirmarCancelacion() {
        if (operacionCancelacion?.isActive == true || consulta?.isActive == true) return
        val seleccion = _cancelacion.value
        val cita = seleccion.cita ?: return
        if (seleccion.requiereVerificacion) {
            comprobarCancelacion()
            return
        }
        if (!puedeCancelar(cita.idCita)) {
            _cancelacion.value = seleccion.copy(
                error = "Solo puedes cancelar citas pendientes o confirmadas con al menos 2 horas de anticipación"
            )
            return
        }
        // Set synchronously so repeated taps cannot start another PUT.
        _cancelacion.value = seleccion.copy(enviando = true, error = null)
        operacionCancelacion = viewModelScope.launch {
            try {
                val resultado = cancelar(cita.idCita)
                currentCoroutineContext().ensureActive()
                val confirmada = resultado.getOrNull()
                if (confirmada?.idCita == cita.idCita && confirmada.estado == "CANCELADA") {
                    _estado.value = _estado.value.copy(
                        citas = _estado.value.citas.map {
                            if (it.idCita == confirmada.idCita) confirmada else it
                        },
                        aviso = "Cita cancelada correctamente."
                    )
                    _cancelacion.value = CancelacionUiState()
                } else {
                    val error = resultado.exceptionOrNull()
                        ?: CancelacionInciertaException("El servidor no confirmó la cancelación")
                    if (error is CancelacionInciertaException) {
                        reconciliarCancelacion(cita.idCita, error.message.orEmpty())
                    } else {
                        _cancelacion.value = seleccion.copy(
                            error = error.message ?: "No se pudo cancelar la cita"
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reconciliarCancelacion(cita.idCita, e.message ?: "No se pudo confirmar la operación")
            }
        }
    }

    fun comprobarCancelacion() {
        if (operacionCancelacion?.isActive == true || consulta?.isActive == true) return
        val seleccion = _cancelacion.value
        val cita = seleccion.cita ?: return
        if (!seleccion.requiereVerificacion) return
        operacionCancelacion = viewModelScope.launch {
            reconciliarCancelacion(cita.idCita, seleccion.error ?: "Operación pendiente de comprobar")
        }
    }

    private suspend fun reconciliarCancelacion(idCita: Long, motivo: String) {
        _estado.value = _estado.value.copy(
            cancelacionesPorVerificar = _estado.value.cancelacionesPorVerificar + idCita,
            aviso = "Hay una cancelación sin confirmar. Actualiza la lista para comprobar su estado antes de repetirla."
        )
        _cancelacion.value = _cancelacion.value.copy(
            enviando = false, verificando = true, requiereVerificacion = true, error = motivo
        )
        val resultado = try {
            consultar()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        currentCoroutineContext().ensureActive()
        val citas = resultado.getOrNull()
        val actual = citas?.find { it.idCita == idCita }
        if (actual == null) {
            _cancelacion.value = _cancelacion.value.copy(
                verificando = false,
                error = "$motivo. No se pudo comprobar el estado. Compruébalo antes de volver a cancelar."
            )
            return
        }
        _estado.value = _estado.value.copy(
            citas = citas,
            cancelacionesPorVerificar = _estado.value.cancelacionesPorVerificar - idCita,
            aviso = if (actual.estado == "CANCELADA") "El servidor confirmó la cancelación." else null
        )
        _cancelacion.value = if (actual.estado == "CANCELADA") {
            CancelacionUiState()
        } else {
            CancelacionUiState(
                cita = actual,
                error = "$motivo. El servidor indica ${actual.estado}. La cancelación no está confirmada."
            )
        }
    }

    fun limpiar() {
        operacionCancelacion?.cancel()
        operacionCancelacion = null
        _cancelacion.value = CancelacionUiState()
        consulta?.cancel()
        consulta = null
        iniciada = false
        _estado.value = MisCitasUiState()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun esCancelable(cita: CitaCreadaResponse, ahora: LocalDateTime): Boolean {
    if (cita.estado != "PENDIENTE" && cita.estado != "CONFIRMADA") return false
    return runCatching {
        val inicio = LocalDateTime.of(LocalDate.parse(cita.fecha), LocalTime.parse(cita.horaInicio))
        !ahora.plusHours(2).isAfter(inicio)
    }.getOrDefault(false)
}
