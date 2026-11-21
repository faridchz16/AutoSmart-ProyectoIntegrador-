package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.repository.AgendaMecanicoRepository
import com.tecsup.autosmart.data.repository.MAX_ANIOS_EXPERIENCIA
import com.tecsup.autosmart.data.repository.MAX_ESPECIALIDADES
import com.tecsup.autosmart.data.repository.MecanicoRepository
import com.tecsup.autosmart.data.repository.OrdenTrabajoRepository
import com.tecsup.autosmart.model.AgendaMecanicoDto
import com.tecsup.autosmart.model.OrdenTrabajoDto
import com.tecsup.autosmart.model.PerfilMecanicoDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Sugerencias comunes de un taller (las mismas que usa el panel web). */
val SUGERENCIAS_ESPECIALIDAD = listOf(
    "Mecánica general", "Frenos", "Suspensión", "Motor", "Electricidad automotriz",
    "Diagnóstico con escáner", "Alineamiento y balanceo", "Transmisión",
    "Aire acondicionado", "Inyección electrónica"
)

data class MecanicoUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val perfil: PerfilMecanicoDto? = null,
    // Resumen del inicio
    val citasHoy: List<AgendaMecanicoDto>? = null,
    val ordenesActivas: List<OrdenTrabajoDto>? = null,
    // Edición del perfil
    val anios: Int = 0,
    val especialidades: List<String> = emptyList(),
    val texto: String = "",
    val guardando: Boolean = false,
    val errorEdicion: String? = null,
    val mensaje: String? = null
) {
    val hayCambios: Boolean
        get() = perfil != null && (anios != (perfil.aniosExperiencia ?: 0) ||
            especialidades.map { it.lowercase() }.sorted() !=
            perfil.especialidades.orEmpty().map { it.lowercase() }.sorted())

    /** Sin especialidades guardadas el mecánico no aparece al reservar. */
    val perfilIncompleto: Boolean get() = perfil != null && perfil.especialidades.isNullOrEmpty()
}

class MecanicoViewModel(
    private val perfiles: MecanicoRepository = MecanicoRepository(),
    private val agenda: AgendaMecanicoRepository = AgendaMecanicoRepository(),
    private val ordenes: OrdenTrabajoRepository = OrdenTrabajoRepository()
) : ViewModel() {
    private val _estado = MutableStateFlow(MecanicoUiState())
    val estado: StateFlow<MecanicoUiState> = _estado.asStateFlow()
    private var carga: Job? = null
    private var guardado: Job? = null

    fun cargarSiNecesario() {
        if (_estado.value.perfil == null && carga?.isActive != true) cargar()
    }

    fun cargar() {
        if (carga?.isActive == true || _estado.value.guardando) return
        carga = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val hoy = LocalDate.now(ZoneId.of("America/Lima")).toString()
            val p = async { perfiles.perfil() }
            val c = async { agenda.listar(hoy) }
            val o = async { ordenes.listar() }
            val perfil = p.await()
            val citas = c.await().getOrNull()
            val activas = o.await().getOrNull()
            _estado.update { actual ->
                val nuevo = perfil.getOrNull()
                actual.copy(
                    cargando = false,
                    error = perfil.exceptionOrNull()?.message,
                    perfil = nuevo ?: actual.perfil,
                    citasHoy = citas?.sortedBy { it.horaInicio } ?: actual.citasHoy,
                    ordenesActivas = activas ?: actual.ordenesActivas,
                    // No pisa lo que el mecánico está editando
                    anios = if (nuevo != null && !actual.hayCambios) nuevo.aniosExperiencia ?: 0 else actual.anios,
                    especialidades = if (nuevo != null && !actual.hayCambios) nuevo.especialidades.orEmpty() else actual.especialidades
                )
            }
        }
    }

    fun cambiarAnios(delta: Int) = editar { it.copy(anios = (it.anios + delta).coerceIn(0, MAX_ANIOS_EXPERIENCIA)) }

    fun editarTexto(texto: String) = editar { it.copy(texto = texto.take(60)) }

    fun agregar(nombre: String = _estado.value.texto) = editar { actual ->
        val limpio = nombre.trim().replace(Regex("\\s+"), " ")
        when {
            limpio.isEmpty() -> actual
            actual.especialidades.any { it.equals(limpio, ignoreCase = true) } ->
                actual.copy(texto = "", errorEdicion = "\"$limpio\" ya está en tu lista.")
            actual.especialidades.size >= MAX_ESPECIALIDADES ->
                actual.copy(errorEdicion = "Puedes registrar hasta $MAX_ESPECIALIDADES especialidades.")
            else -> actual.copy(especialidades = actual.especialidades + limpio, texto = "")
        }
    }

    fun quitar(nombre: String) = editar { it.copy(especialidades = it.especialidades.filterNot { e -> e == nombre }) }

    fun descartar() = editar { actual ->
        val p = actual.perfil ?: return@editar actual
        actual.copy(anios = p.aniosExperiencia ?: 0, especialidades = p.especialidades.orEmpty(), texto = "")
    }

    fun guardar() {
        val actual = _estado.value
        if (actual.guardando || actual.perfil == null) return
        MecanicoRepository.validar(actual.anios, MecanicoRepository.limpiar(actual.especialidades))?.let { error ->
            _estado.update { it.copy(errorEdicion = error) }
            return
        }
        guardado = viewModelScope.launch {
            _estado.update { it.copy(guardando = true, errorEdicion = null, mensaje = null) }
            perfiles.guardar(actual.anios, actual.especialidades)
                .onSuccess { p ->
                    _estado.update {
                        it.copy(guardando = false, perfil = p, anios = p.aniosExperiencia ?: 0,
                            especialidades = p.especialidades.orEmpty(), mensaje = "Perfil actualizado correctamente.")
                    }
                }
                .onFailure { e ->
                    _estado.update { it.copy(guardando = false, errorEdicion = e.message ?: "No se pudo guardar tu perfil.") }
                }
        }
    }

    fun limpiarMensaje() = _estado.update { it.copy(mensaje = null) }

    private fun editar(cambio: (MecanicoUiState) -> MecanicoUiState) {
        if (_estado.value.guardando) return
        _estado.update { cambio(it.copy(errorEdicion = null, mensaje = null)) }
    }

    fun limpiar() {
        carga?.cancel()
        guardado?.cancel()
        _estado.value = MecanicoUiState()
    }
}
