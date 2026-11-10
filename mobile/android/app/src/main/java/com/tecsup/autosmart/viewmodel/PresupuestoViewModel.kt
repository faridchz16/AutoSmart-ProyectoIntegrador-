package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.model.DecisionPresupuesto
import com.tecsup.autosmart.model.PresupuestoResponse
import com.tecsup.autosmart.data.repository.PresupuestoRepository
import com.tecsup.autosmart.data.repository.FotoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PresupuestoEstado(
    val idCita: Long = 0, val cargando: Boolean = true,
    val presupuesto: PresupuestoResponse? = null, val error: String? = null,
    val enviandoDecision: Boolean = false,
    val errorDecision: String? = null
)

class PresupuestoViewModel(
    private val consultar: suspend (Long) -> Result<PresupuestoResponse?> = { PresupuestoRepository().obtener(it) },
    private val verFoto: suspend (Long) -> Result<ByteArray> = { FotoRepository().obtener(it) },
    private val enviarDecision: suspend (PresupuestoResponse, DecisionPresupuesto) -> Result<PresupuestoResponse> =
        { presupuesto, decision -> PresupuestoRepository().decidir(presupuesto, decision) }
) : ViewModel() {
    private val _estado = MutableStateFlow(PresupuestoEstado())
    val estado: StateFlow<PresupuestoEstado> = _estado
    private var iniciada = false
    private var tarea: Job? = null

    fun iniciar(idCita: Long) {
        if (iniciada && _estado.value.idCita == idCita) return
        cargar(idCita)
    }

    fun actualizar() { cargar(_estado.value.idCita) }

    private fun cargar(idCita: Long) {
        if (_estado.value.enviandoDecision) return
        if (tarea?.isActive == true && _estado.value.idCita == idCita) return
        tarea?.cancel()
        iniciada = true
        _estado.value = PresupuestoEstado(idCita = idCita)
        tarea = viewModelScope.launch {
            val result = try { consultar(idCita) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            currentCoroutineContext().ensureActive()
            result.onSuccess { _estado.value = PresupuestoEstado(idCita, cargando = false, presupuesto = it) }
                .onFailure { _estado.value = PresupuestoEstado(idCita, cargando = false,
                    error = it.message ?: "No se pudo consultar el presupuesto.") }
        }
    }

    fun decidir(idPresupuesto: Long, decision: DecisionPresupuesto) {
        val actual = _estado.value
        val presupuesto = actual.presupuesto ?: return
        if (actual.cargando || actual.enviandoDecision || actual.errorDecision != null ||
            presupuesto.idPresupuesto != idPresupuesto || presupuesto.estado != "PENDIENTE") return

        _estado.value = actual.copy(enviandoDecision = true)
        tarea = viewModelScope.launch {
            val resultado = try { enviarDecision(presupuesto, decision) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            currentCoroutineContext().ensureActive()
            _estado.value = resultado.fold(
                onSuccess = { actual.copy(presupuesto = it) },
                onFailure = { actual.copy(errorDecision = it.message ?: "No se pudo confirmar la decisión.") }
            )
        }
    }

    suspend fun obtenerFoto(idFoto: Long): Result<ByteArray> {
        val permitida = _estado.value.presupuesto?.deficiencias?.any { d -> d.fotos?.any { it.idFoto == idFoto } == true } == true
        if (!permitida) return Result.failure(Exception("La foto no pertenece a este presupuesto."))
        return verFoto(idFoto)
    }

    fun limpiar() {
        tarea?.cancel(); tarea = null; iniciada = false
        _estado.value = PresupuestoEstado()
    }
}
