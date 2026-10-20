package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.MecanicoDisponible
import com.tecsup.autosmart.data.model.Turno
import com.tecsup.autosmart.data.network.RetrofitClient
import com.tecsup.autosmart.data.network.mensajeDeError

class ReservaRepositoryRetrofit : ReservaRepository {

    override suspend fun mecanicosPorServicio(idServicio: Long): Result<List<MecanicoDisponible>> =
        try {
            val res = RetrofitClient.reservaApi.mecanicosPorServicio(idServicio)
            val body = res.body()
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
                Result.failure(Exception(mensajeDeError(res.errorBody()?.string(), res.code())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión"))
        }

    override suspend fun turnos(idServicio: Long, fecha: String, idMecanico: Int): Result<List<Turno>> =
        try {
            val res = RetrofitClient.reservaApi.turnos(idServicio, fecha, idMecanico)
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
                Result.failure(Exception(mensajeDeError(res.errorBody()?.string(), res.code())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión"))
        }
}