package com.tecsup.autosmart.data.repository

import com.google.gson.JsonParser
import com.tecsup.autosmart.model.*
import com.tecsup.autosmart.data.remote.PresupuestoApi
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.mensajeDeError
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal

class PresupuestoRepository(private val api: PresupuestoApi = RetrofitClient.presupuestoApi) {
    suspend fun decidir(
        presupuesto: PresupuestoResponse,
        decision: DecisionPresupuesto
    ): Result<PresupuestoResponse> = try {
        require(presupuesto.idPresupuesto > 0 && presupuesto.estado == "PENDIENTE") {
            "El presupuesto no admite una decisión."
        }
        val response = api.decidir(presupuesto.idPresupuesto, DecisionPresupuestoRequest(decision))
        if (!response.isSuccessful) {
            Result.failure(Exception(mensajeDeError(response.errorBody()?.string(), response.code())))
        } else {
            val body = response.body()
            require(body != null && presupuestoValido(body, presupuesto.idCita) &&
                body.idPresupuesto == presupuesto.idPresupuesto && body.idOrden == presupuesto.idOrden &&
                body.estado == decision.name && !body.fechaDecision.isNullOrBlank()) {
                "El servidor no confirmó la decisión."
            }
            Result.success(body)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtener(idCita: Long): Result<PresupuestoResponse?> = try {
        require(idCita > 0) { "La cita no es válida." }
        val response = api.obtener(idCita)
        val error = response.errorBody()?.string()
        val noExiste = response.code() == 404 && runCatching {
            val json = JsonParser.parseString(error).asJsonObject
            listOf("error", "message", "mensaje").any { key ->
                json.get(key)?.takeIf { it.isJsonPrimitive }?.asString in setOf(
                    "La cita aun no tiene presupuesto", "La orden aun no tiene presupuesto")
            }
        }.getOrDefault(false)
        when {
            noExiste -> Result.success(null)
            !response.isSuccessful -> Result.failure(Exception(mensajeDeError(error, response.code())))
            else -> {
                val body = response.body()
                require(body != null && presupuestoValido(body, idCita)) { "El servidor no devolvió un presupuesto válido para esta cita." }
                Result.success(body)
            }
        }
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { Result.failure(Exception(e.message ?: "No se pudo consultar el presupuesto. Revisa tu conexión.", e)) }
}

internal fun presupuestoValido(p: PresupuestoResponse, idCita: Long): Boolean {
    if (idCita <= 0 || p.idCita != idCita || p.idPresupuesto <= 0 || p.idOrden <= 0 ||
        p.estado !in setOf("PENDIENTE", "APROBADO", "RECHAZADO") || p.descripcion.isNullOrBlank() ||
        p.fechaCreacion.isNullOrBlank() || p.items.isNullOrEmpty() || p.total == null || p.total.signum() < 0 || p.deficiencias == null) return false
    val total = p.items.fold(BigDecimal.ZERO) { suma, item ->
        if (item.tipo !in setOf("MANO_OBRA", "REPUESTO") || item.descripcion.isNullOrBlank() ||
            item.cantidad == null || item.cantidad <= 0 || item.precioUnitario == null || item.precioUnitario.signum() < 0 ||
            item.subtotal == null || item.subtotal.signum() < 0 ||
            item.precioUnitario.multiply(BigDecimal.valueOf(item.cantidad.toLong())).compareTo(item.subtotal) != 0) return false
        suma.add(item.subtotal)
    }
    if (total.compareTo(p.total) != 0) return false
    if (p.deficiencias.distinctBy { it.idDeficiencia }.size != p.deficiencias.size) return false
    return p.deficiencias.all { d ->
        d.idDeficiencia > 0 && d.idOrden == p.idOrden && d.categoria in categoriasDeficiencia &&
            d.severidad in severidadesDeficiencia && !d.descripcion.isNullOrBlank() && d.fotos != null &&
            d.fotos.distinctBy { it.idFoto }.size == d.fotos.size && d.fotos.all { it.idFoto > 0 && it.url == "/photos/${it.idFoto}" }
    }
}
