
package com.tecsup.autosmart.data.repository

import android.util.Log
import com.tecsup.autosmart.model.MecanicoDisponible
import com.tecsup.autosmart.model.Turno
import com.tecsup.autosmart.data.remote.RetrofitClient
import com.tecsup.autosmart.data.remote.mensajeDeError

class ReservaRepositoryRetrofit : ReservaRepository {

    // HU-24: Obtener mecánicos reales desde el backend
    override suspend fun mecanicosPorServicio(
        idServicio: Long
    ): Result<List<MecanicoDisponible>> = try {

        Log.d(
            "HU24_MECANICOS",
            "Consultando mecánicos del servicio ID: $idServicio"
        )

        val res = RetrofitClient.reservaApi.mecanicosPorServicio(idServicio)
        val body = res.body()

        Log.d(
            "HU24_MECANICOS",
            "HTTP: ${res.code()} | Cantidad: ${body?.size ?: "sin cuerpo"}"
        )

        if (res.isSuccessful && body != null) {
            Result.success(
                body.map { mecanicoDto ->
                    MecanicoDisponible(
                        idMecanico = mecanicoDto.idMecanico,
                        nombre = mecanicoDto.nombre,
                        aniosExperiencia = mecanicoDto.aniosExperiencia ?: 0,
                        especialidades = mecanicoDto.especialidades.orEmpty()
                    )
                }
            )
        } else {
            val mensaje = mensajeDeError(
                res.errorBody()?.string(),
                res.code()
            )

            Log.e("HU24_MECANICOS", "Error HTTP: $mensaje")
            Result.failure(Exception(mensaje))
        }

    } catch (e: Exception) {
        Log.e(
            "HU24_MECANICOS",
            "Error real al consultar mecánicos",
            e
        )

        Result.failure(
            Exception(
                e.message ?: "Error desconocido al consultar mecánicos",
                e
            )
        )
    }

    // HU-25: Obtener turnos (sin cambiar su lógica original)
    override suspend fun turnos(
        idServicio: Long,
        fecha: String,
        idMecanico: Int
    ): Result<List<Turno>> = try {

        val res = RetrofitClient.reservaApi.turnos(
            idServicio,
            fecha,
            idMecanico
        )

        val body = res.body()

        if (res.isSuccessful && body != null) {
            Result.success(
                body.map { turnoDto ->
                    Turno(
                        hora = turnoDto.hora.take(5),
                        disponible = turnoDto.disponible ?: true
                    )
                }
            )
        } else {
            Result.failure(
                Exception(
                    mensajeDeError(
                        res.errorBody()?.string(),
                        res.code()
                    )
                )
            )
        }

    } catch (e: Exception) {
        Result.failure(
            Exception("No se pudo conectar con el servidor. Revisa tu conexión")
        )
    }
}
