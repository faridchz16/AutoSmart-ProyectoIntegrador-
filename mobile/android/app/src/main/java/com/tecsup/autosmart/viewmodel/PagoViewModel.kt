package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.data.repository.ComprobantePdf
import com.tecsup.autosmart.data.repository.PagoRepository
import com.tecsup.autosmart.data.repository.YaCalificadoException
import com.tecsup.autosmart.model.CalificacionDto
import com.tecsup.autosmart.data.repository.PagoSinConfirmarException
import com.tecsup.autosmart.model.CostoOrdenDto
import com.tecsup.autosmart.model.MetodoPagoDto
import com.tecsup.autosmart.model.PagoDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tarjetas de prueba de la pasarela sandbox (T63.1): nunca se piden datos reales de tarjeta. */
data class TarjetaPrueba(val token: String, val nombre: String, val resultado: String)

val TARJETAS_PRUEBA = listOf(
    TarjetaPrueba("pm_card_visa", "Visa •••• 4242", "Se aprueba"),
    TarjetaPrueba("pm_card_mastercard", "Mastercard •••• 4444", "Se aprueba"),
    TarjetaPrueba("pm_card_chargeDeclined", "Visa •••• 0002", "Se rechaza"),
    TarjetaPrueba("pm_card_chargeDeclinedInsufficientFunds", "Visa •••• 9995", "Fondos insuficientes"),
    TarjetaPrueba("pm_card_chargeDeclinedExpiredCard", "Visa •••• 0069", "Tarjeta vencida")
)

const val METODO_TARJETA = "TARJETA"
const val METODO_YAPE = "YAPE_PLIN"

data class PagoUiState(
    val idOrden: Long = 0,
    /** Cuando se abre desde Mis citas: la orden se busca con la cita y la placa. */
    val idCita: Long = 0,
    val placa: String = "",
    val cargando: Boolean = true,
    val error: String? = null,
    val costo: CostoOrdenDto? = null,
    val metodos: List<MetodoPagoDto> = emptyList(),
    val errorMetodos: String? = null,
    val metodoElegido: String? = null,
    val tarjeta: String? = null,
    val codigoYape: String = "",
    val errorDatos: String? = null,
    val pagando: Boolean = false,
    /** Último intento: APROBADO o RECHAZADO (con motivo). */
    val resultado: PagoDto? = null,
    /** La red falló al pagar: hay que comprobar antes de reintentar (evita doble cobro). */
    val sinConfirmar: String? = null,
    // T64.2: comprobante en PDF
    val descargando: Boolean = false,
    val pdfListo: ComprobantePdf? = null,
    val mensajeComprobante: String? = null,
    // T65.2: calificación al mecánico
    val estrellas: Int = 0,
    val comentario: String = "",
    val calificando: Boolean = false,
    val calificacion: CalificacionDto? = null,
    val yaCalificado: Boolean = false,
    val errorCalificacion: String? = null
) {
    /** Id del pago aprobado (recién hecho o ya registrado) para pedir su comprobante. */
    val idPagoAprobado: Long?
        get() = resultado?.takeIf { it.estado == "APROBADO" }?.idPago ?: costo?.pago?.idPago

    /** Se puede calificar un servicio finalizado o pagado (una sola vez). */
    val puedeCalificar: Boolean
        get() = costo != null && (costo.estadoCita in setOf("FINALIZADA", "PAGADO") || pagado)

    val metodo: MetodoPagoDto? get() = metodos.firstOrNull { it.codigo == metodoElegido }
    val pagado: Boolean get() = costo?.pago != null || resultado?.estado == "APROBADO"
}

class PagoViewModel(private val repositorio: PagoRepository = PagoRepository()) : ViewModel() {
    private val _estado = MutableStateFlow(PagoUiState())
    val estado: StateFlow<PagoUiState> = _estado.asStateFlow()
    private var carga: Job? = null
    private var pago: Job? = null

    fun iniciar(idOrden: Long) {
        if (_estado.value.idOrden == idOrden && (_estado.value.costo != null || carga?.isActive == true)) return
        pago?.cancel()
        _estado.value = PagoUiState(idOrden = idOrden)
        cargar()
    }

    /** Desde Mis citas solo se conoce la cita: primero se ubica su orden de trabajo. */
    fun iniciarDesdeCita(idCita: Long, placa: String) {
        val actual = _estado.value
        if (actual.idCita == idCita && (actual.costo != null || carga?.isActive == true)) return
        pago?.cancel()
        _estado.value = PagoUiState(idCita = idCita, placa = placa)
        cargar()
    }

    fun cargar() {
        if (carga?.isActive == true || _estado.value.pagando) return
        carga = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, errorMetodos = null) }
            if (_estado.value.idOrden <= 0) {
                val orden = repositorio.ordenDeCita(_estado.value.idCita, _estado.value.placa)
                val id = orden.getOrNull()
                if (id == null) {
                    _estado.update { it.copy(cargando = false, error = orden.exceptionOrNull()?.message) }
                    return@launch
                }
                _estado.update { it.copy(idOrden = id) }
            }
            val idOrden = _estado.value.idOrden
            val costo = async { repositorio.costo(idOrden) }
            val metodos = async { repositorio.metodos() }
            val c = costo.await()
            val m = metodos.await()
            _estado.update { actual ->
                val lista = m.getOrNull().orEmpty()
                actual.copy(
                    cargando = false,
                    costo = c.getOrNull() ?: actual.costo,
                    error = c.exceptionOrNull()?.message,
                    metodos = lista.ifEmpty { actual.metodos },
                    errorMetodos = m.exceptionOrNull()?.message,
                    metodoElegido = actual.metodoElegido?.takeIf { cod -> lista.any { it.codigo == cod } }
                        ?: lista.firstOrNull { it.enLinea }?.codigo
                )
            }
        }
    }

    fun elegirMetodo(codigo: String) {
        if (_estado.value.pagando) return
        _estado.update { it.copy(metodoElegido = codigo, errorDatos = null, resultado = null) }
    }

    fun elegirTarjeta(token: String) {
        if (_estado.value.pagando) return
        _estado.update { it.copy(tarjeta = token, errorDatos = null) }
    }

    fun editarCodigoYape(texto: String) {
        if (_estado.value.pagando) return
        _estado.update { it.copy(codigoYape = texto.filter(Char::isDigit).take(6), errorDatos = null) }
    }

    /** T63.2: valida los datos y envía el pago. */
    fun pagar() {
        val actual = _estado.value
        val costo = actual.costo ?: return
        if (actual.pagando || pago?.isActive == true || actual.sinConfirmar != null || actual.pagado) return
        if (!costo.puedePagar) {
            _estado.update { it.copy(errorDatos = "Esta orden todavía no se puede pagar.") }
            return
        }
        val metodo = actual.metodo
        val token = when {
            metodo == null -> null.also { error("Elige un método de pago.") }
            !metodo.enLinea -> null.also { error("El pago en efectivo se hace en el taller.") }
            metodo.codigo == METODO_TARJETA -> actual.tarjeta ?: null.also { error("Elige una tarjeta de prueba.") }
            metodo.codigo == METODO_YAPE -> actual.codigoYape.takeIf { it.length == 6 }
                ?: null.also { error("Escribe el código de aprobación de 6 dígitos.") }
            else -> null.also { error("Este método no está disponible en la app.") }
        } ?: return

        pago = viewModelScope.launch {
            _estado.update { it.copy(pagando = true, errorDatos = null, resultado = null) }
            repositorio.pagar(costo.idOrden, metodo!!.codigo, token)
                .onSuccess { respuesta ->
                    _estado.update { it.copy(pagando = false, resultado = respuesta, codigoYape = "") }
                    if (respuesta.estado == "APROBADO") recargarCosto()
                }
                .onFailure { e ->
                    _estado.update {
                        if (e is PagoSinConfirmarException) it.copy(pagando = false, sinConfirmar = e.message)
                        else it.copy(pagando = false, errorDatos = e.message ?: "No se pudo realizar el pago.")
                    }
                    // Si el servidor dice que ya está pagada (409), se muestra el pago existente.
                    if (e !is PagoSinConfirmarException) recargarCosto()
                }
        }
    }

    /** Tras un rechazo: vuelve al formulario con el mismo método para reintentar. */
    fun reintentar() {
        if (_estado.value.pagando) return
        _estado.update { it.copy(resultado = null, errorDatos = null) }
    }

    /** Después de un error de red: consulta el costo para saber si el pago sí se registró. */
    fun comprobarEstado() {
        if (carga?.isActive == true) return
        carga = viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            repositorio.costo(_estado.value.idOrden)
                .onSuccess { costo -> _estado.update { it.copy(cargando = false, costo = costo, sinConfirmar = null, error = null) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, sinConfirmar = e.message ?: it.sinConfirmar) } }
        }
    }

    private fun recargarCosto() {
        viewModelScope.launch {
            repositorio.costo(_estado.value.idOrden).onSuccess { costo -> _estado.update { it.copy(costo = costo) } }
        }
    }

    private fun error(mensaje: String) {
        _estado.update { it.copy(errorDatos = mensaje) }
    }

    /** T64.2: descarga el PDF; la pantalla lo guarda en Descargas y lo abre. */
    fun descargarComprobante() {
        val idPago = _estado.value.idPagoAprobado ?: return
        if (_estado.value.descargando) return
        viewModelScope.launch {
            _estado.update { it.copy(descargando = true, mensajeComprobante = null) }
            repositorio.comprobante(idPago)
                .onSuccess { pdf -> _estado.update { it.copy(descargando = false, pdfListo = pdf) } }
                .onFailure { e -> _estado.update { it.copy(descargando = false, mensajeComprobante = e.message) } }
        }
    }

    fun comprobanteGuardado(mensaje: String) = _estado.update { it.copy(pdfListo = null, mensajeComprobante = mensaje) }

    // T65.2: calificación con estrellas y comentario
    fun elegirEstrellas(n: Int) {
        if (!_estado.value.calificando) _estado.update { it.copy(estrellas = n.coerceIn(1, 5), errorCalificacion = null) }
    }

    fun editarComentario(texto: String) {
        if (!_estado.value.calificando) _estado.update { it.copy(comentario = texto.take(500), errorCalificacion = null) }
    }

    fun marcarYaCalificado() = _estado.update { it.copy(yaCalificado = true) }

    fun calificar() {
        val actual = _estado.value
        val costo = actual.costo ?: return
        if (actual.calificando || actual.calificacion != null || actual.yaCalificado) return
        if (actual.estrellas !in 1..5) {
            _estado.update { it.copy(errorCalificacion = "Elige de 1 a 5 estrellas.") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(calificando = true, errorCalificacion = null) }
            repositorio.calificar(costo.idOrden, actual.estrellas, actual.comentario)
                .onSuccess { c -> _estado.update { it.copy(calificando = false, calificacion = c) } }
                .onFailure { e ->
                    _estado.update {
                        if (e is YaCalificadoException) it.copy(calificando = false, yaCalificado = true)
                        else it.copy(calificando = false, errorCalificacion = e.message ?: "No se pudo enviar tu calificación.")
                    }
                }
        }
    }

    fun limpiar() {
        carga?.cancel()
        // Un pago en curso no se cancela a medias: se deja terminar en el servidor.
        _estado.value = PagoUiState()
    }
}
