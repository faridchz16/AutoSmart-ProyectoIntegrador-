package com.tecsup.autosmart.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.autosmart.model.*
import com.tecsup.autosmart.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class FotosDeficienciaEstado(
    val idOrden: Long = 0, val idDeficiencia: Long = 0, val editable: Boolean = false,
    val cargando: Boolean = true, val cargado: Boolean = false, val subiendo: Boolean = false,
    val capturando: Boolean = false, val fotos: List<FotoReferenciaDto> = emptyList(),
    val archivo: String? = null, val camara: String? = null, val verificar: Boolean = false,
    val error: String? = null, val mensaje: String? = null
) {
    val ocupado get() = cargando || subiendo || capturando
    val puedeCapturar get() = editable && cargado && !ocupado && !verificar && fotos.size < MAXIMO_FOTOS_DEFICIENCIA
}

class FotosDeficienciaViewModel @JvmOverloads constructor(
    private val saved: SavedStateHandle,
    private val consultar: suspend (Long, Long) -> Result<List<FotoReferenciaDto>> = { orden, def -> FotoRepository().listar(orden, def) },
    private val enviar: suspend (Long, Long, File) -> Result<FotoCreadaResponse> = { orden, def, file -> FotoRepository().subir(orden, def, file) },
    private val ver: suspend (Long) -> Result<ByteArray> = { FotoRepository().obtener(it) }
) : ViewModel() {
    private var iniciado = false
    private val _estado = MutableStateFlow(FotosDeficienciaEstado(
        idOrden = saved.get<Long>("foto_orden") ?: 0,
        idDeficiencia = saved.get<Long>("foto_deficiencia") ?: 0,
        archivo = saved["foto_archivo"], camara = saved["foto_camara"],
        capturando = saved.get<String>("foto_camara") != null,
        verificar = saved.get<Boolean>("foto_envio") == true
    ))
    val estado: StateFlow<FotosDeficienciaEstado> = _estado
    private var tarea: Job? = null

    fun iniciar(idOrden: Long, idDeficiencia: Long, editable: Boolean) {
        if (iniciado && _estado.value.idOrden == idOrden && _estado.value.idDeficiencia == idDeficiencia) return
        iniciado = true
        tarea?.cancel()
        val recuperar = saved.get<Long>("foto_orden") == idOrden && saved.get<Long>("foto_deficiencia") == idDeficiencia
        _estado.value = FotosDeficienciaEstado(idOrden, idDeficiencia, editable,
            archivo = if (recuperar) saved["foto_archivo"] else null,
            camara = if (recuperar) saved["foto_camara"] else null,
            capturando = recuperar && saved.get<String>("foto_camara") != null,
            verificar = recuperar && saved.get<Boolean>("foto_envio") == true)
        guardarEstado()
        cargar()
    }

    private fun guardarEstado() {
        val actual = _estado.value
        saved["foto_orden"] = actual.idOrden; saved["foto_deficiencia"] = actual.idDeficiencia
        saved["foto_archivo"] = actual.archivo; saved["foto_camara"] = actual.camara
        saved["foto_envio"] = actual.verificar || actual.subiendo
    }

    fun cargar() {
        val actual = _estado.value
        if (actual.idOrden <= 0 || actual.idDeficiencia <= 0 || actual.subiendo || tarea?.isActive == true) return
        _estado.value = actual.copy(cargando = true, error = null, mensaje = null)
        tarea = viewModelScope.launch {
            val result = try { consultar(actual.idOrden, actual.idDeficiencia) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            currentCoroutineContext().ensureActive()
            // Camera callbacks can return while the initial GET is running.
            val vigente = _estado.value
            result.onSuccess { fotos ->
                _estado.value = vigente.copy(cargando = false, cargado = true, fotos = fotos, error = null,
                    mensaje = if (vigente.verificar) "Lista actualizada. Revisa las fotos del servidor antes de descartar la captura pendiente. No se reenviará automáticamente." else null)
            }.onFailure {
                _estado.value = vigente.copy(cargando = false, error = it.message ?: "No se pudieron consultar las fotos.")
            }
        }
    }

    fun prepararCamara(file: File): Boolean {
        if (!_estado.value.puedeCapturar) return false
        _estado.value = _estado.value.copy(camara = file.absolutePath, capturando = true, error = null, mensaje = null)
        guardarEstado()
        return true
    }

    fun capturaTerminada(exito: Boolean, error: String? = null) {
        val actual = _estado.value
        if (!actual.capturando) return
        val file = actual.camara?.let(::File)
        val fallo = if (exito && file != null) runCatching { validarArchivoFoto(file) }.exceptionOrNull()?.message else error
        _estado.value = actual.copy(capturando = false, camara = null,
            archivo = if (exito && file != null && fallo == null) file.absolutePath else actual.archivo,
            error = fallo, mensaje = if (exito && fallo == null) "Foto capturada. Revisa la vista previa y envíala." else null)
        guardarEstado()
    }

    fun informarError(message: String) { _estado.value = _estado.value.copy(error = message) }

    fun descartarCaptura() {
        if (_estado.value.ocupado) return
        _estado.value = _estado.value.copy(archivo = null, verificar = false, error = null, mensaje = null)
        guardarEstado()
    }

    fun subir() {
        val actual = _estado.value
        if (!actual.puedeCapturar || actual.archivo == null || tarea?.isActive == true) return
        _estado.value = actual.copy(subiendo = true, error = null, mensaje = null)
        guardarEstado()
        tarea = viewModelScope.launch {
            val result = try { enviar(actual.idOrden, actual.idDeficiencia, File(actual.archivo)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(SubidaFotoInciertaException("Comprueba las fotos antes de reenviar.", e)) }
            currentCoroutineContext().ensureActive()
            result.onSuccess { body ->
                _estado.value = actual.copy(subiendo = false, archivo = null, verificar = false,
                    fotos = actual.fotos.filterNot { it.idFoto == body.idFoto } + FotoReferenciaDto(body.idFoto, body.url),
                    mensaje = "Foto guardada por el servidor.")
            }.onFailure {
                _estado.value = actual.copy(subiendo = false, verificar = it is SubidaFotoInciertaException,
                    error = it.message ?: "No se pudo subir. La captura se conserva.")
            }
            guardarEstado()
        }
    }

    suspend fun obtener(idFoto: Long): Result<ByteArray> {
        if (_estado.value.fotos.none { it.idFoto == idFoto }) return Result.failure(Exception("La foto no pertenece a la lista consultada."))
        return ver(idFoto)
    }

    fun limpiar() {
        tarea?.cancel(); tarea = null; iniciado = false
        _estado.value = FotosDeficienciaEstado()
        // Keep any pending capture in SavedStateHandle until another deficiency is opened.
    }
}
