package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.remote.PagoApi
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.VehiculoApi
import com.google.gson.JsonParser
import com.tecsup.autosmart.data.remote.mensajeDeError
import com.tecsup.autosmart.model.CalificacionDto
import com.tecsup.autosmart.model.CalificacionRequest
import com.tecsup.autosmart.model.CostoOrdenDto
import com.tecsup.autosmart.model.MetodoPagoDto
import com.tecsup.autosmart.model.PagoDto
import com.tecsup.autosmart.model.PagoRequest
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Toma el nombre del PDF de Content-Disposition (filename="...") o arma uno por defecto. */
fun nombreDeArchivo(disposicion: String?, idPago: Long): String {
    val nombre = disposicion?.let { Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]+)\"?").find(it)?.groupValues?.get(1) }
        ?.substringAfterLast('/')?.trim()
    return nombre?.takeIf { it.endsWith(".pdf", ignoreCase = true) } ?: "comprobante-$idPago.pdf"
}

/** Comprobante PDF descargado (T64.2). */
class ComprobantePdf(val nombre: String, val bytes: ByteArray)

/** El servicio ya tenía una calificación (409 del backend). */
class YaCalificadoException(mensaje: String) : Exception(mensaje)

/** Error de red al pagar: el cobro pudo hacerse, hay que comprobar antes de reintentar. */
class PagoSinConfirmarException(causa: Throwable) :
    Exception("No pudimos confirmar el pago. Comprueba el estado antes de volver a intentarlo.", causa)

class PagoRepository(
    private val api: PagoApi = RetrofitClient.pagoApi,
    private val vehiculos: VehiculoApi = RetrofitClient.vehiculoApi
) {
    /** T61.2: GET /work-orders/{id}/cost */
    suspend fun costo(idOrden: Long): Result<CostoOrdenDto> = consultar("No se pudo consultar el costo. Revisa tu conexión.") {
        require(idOrden > 0) { "La orden no es válida." }
        val response = api.costo(idOrden)
        if (!response.isSuccessful) throw Exception(errorDe(response.errorBody()?.string(), response.code()))
        val body = response.body()
        require(body != null && body.idOrden == idOrden && body.total != null && body.total.signum() >= 0) {
            "El servidor no devolvió un costo válido para esta orden."
        }
        body
    }

    /** T62.2: GET /payment-methods (solo los habilitados) */
    suspend fun metodos(): Result<List<MetodoPagoDto>> = consultar("No se pudieron cargar los métodos de pago.") {
        val response = api.metodos()
        if (!response.isSuccessful) throw Exception(errorDe(response.errorBody()?.string(), response.code()))
        val lista = response.body().orEmpty().filter { it.codigo.isNotBlank() }.distinctBy { it.codigo }
        require(lista.isNotEmpty()) { "No hay métodos de pago disponibles por ahora." }
        lista
    }

    /**
     * T63.2: POST /payments. Un RECHAZADO también es una respuesta válida (201) con su motivo.
     * Si la red falla después de enviar, devuelve [PagoSinConfirmarException] para no cobrar dos veces.
     */
    suspend fun pagar(idOrden: Long, metodo: String, tokenPago: String): Result<PagoDto> = try {
        require(idOrden > 0) { "La orden no es válida." }
        require(metodo.isNotBlank() && tokenPago.isNotBlank()) { "Completa los datos del pago." }
        val response = api.pagar(PagoRequest(idOrden, metodo, tokenPago.trim()))
        if (!response.isSuccessful) {
            Result.failure(Exception(errorDe(response.errorBody()?.string(), response.code())))
        } else {
            val body = response.body()
            require(body != null && body.idOrden == idOrden && body.estado in setOf("APROBADO", "RECHAZADO")) {
                "El servidor no confirmó el pago."
            }
            Result.success(body)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(PagoSinConfirmarException(e))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Busca la orden de trabajo de una cita (Mis citas solo conoce la cita):
     * revisa el historial del vehículo con esa placa.
     */
    suspend fun ordenDeCita(idCita: Long, placa: String): Result<Long> = consultar("No se pudo ubicar la orden de esta cita.") {
        require(idCita > 0) { "La cita no es válida." }
        val lista = vehiculos.listarVehiculos()
        if (!lista.isSuccessful) throw Exception(errorDe(lista.errorBody()?.string(), lista.code()))
        val vehiculo = lista.body().orEmpty().firstOrNull { it.placa.equals(placa.trim(), ignoreCase = true) }
            ?: throw Exception("No encontramos el vehículo $placa en tu cuenta.")
        val historial = vehiculos.historial(vehiculo.idVehiculo)
        if (!historial.isSuccessful) throw Exception(errorDe(historial.errorBody()?.string(), historial.code()))
        historial.body()?.mantenimientos.orEmpty().firstOrNull { it.idCita == idCita }?.idOrden
            ?: throw Exception("Esta cita todavía no tiene una orden de trabajo.")
    }

    /** Mensaje del backend ({"error": "..."}) leído con Gson; si no hay, el mensaje genérico de la app. */
    private fun errorDe(cuerpo: String?, codigo: Int): String = runCatching {
        val json = JsonParser.parseString(cuerpo).asJsonObject
        listOf("error", "mensaje", "message").firstNotNullOfOrNull { clave ->
            json.get(clave)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
        }
    }.getOrNull() ?: mensajeDeError(cuerpo, codigo)

    /** T64.2: GET /payments/{id}/receipt devuelve el PDF del pago aprobado. */
    suspend fun comprobante(idPago: Long): Result<ComprobantePdf> = consultar("No se pudo descargar el comprobante. Revisa tu conexión.") {
        require(idPago > 0) { "El pago no es válido." }
        val r = api.comprobante(idPago)
        if (!r.isSuccessful) throw Exception(errorDe(r.errorBody()?.string(), r.code()))
        val bytes = r.body()?.bytes() ?: ByteArray(0)
        require(bytes.size > 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "%PDF") {
            "El servidor no devolvió un comprobante válido."
        }
        ComprobantePdf(nombreDeArchivo(r.headers()["Content-Disposition"], idPago), bytes)
    }

    /** T65.2: POST /work-orders/{id}/rating. Un 409 significa que ya se calificó. */
    suspend fun calificar(idOrden: Long, estrellas: Int, comentario: String): Result<CalificacionDto> =
        consultar("No se pudo enviar tu calificación. Revisa tu conexión.") {
            require(idOrden > 0) { "La orden no es válida." }
            require(estrellas in 1..5) { "Elige de 1 a 5 estrellas." }
            val texto = comentario.trim().takeIf { it.isNotEmpty() }
            require((texto?.length ?: 0) <= 500) { "El comentario puede tener hasta 500 caracteres." }
            val r = api.calificar(idOrden, CalificacionRequest(estrellas, texto))
            if (r.code() == 409) throw YaCalificadoException(errorDe(r.errorBody()?.string(), r.code()))
            if (!r.isSuccessful) throw Exception(errorDe(r.errorBody()?.string(), r.code()))
            val body = r.body()
            require(body != null && body.idOrden == idOrden && body.estrellas == estrellas) {
                "El servidor no confirmó tu calificación."
            }
            body
        }

    private suspend fun <T> consultar(mensajeRed: String, bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(Exception(mensajeRed, e))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
