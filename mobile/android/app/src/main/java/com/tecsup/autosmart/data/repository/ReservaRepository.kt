package com.tecsup.autosmart.data.repository

import com.tecsup.autosmart.data.model.MecanicoDisponible
import com.tecsup.autosmart.data.model.Turno

interface ReservaRepository {
    suspend fun mecanicosPorServicio(idServicio: Long): Result<List<MecanicoDisponible>>
    suspend fun turnos(idServicio: Long, fecha: String, idMecanico: Int): Result<List<Turno>>
}

class ReservaRepositoryFake : ReservaRepository {
    override suspend fun mecanicosPorServicio(idServicio: Long): Result<List<MecanicoDisponible>> {
        return Result.success(
            listOf(
                MecanicoDisponible(
                    idMecanico = 1,
                    nombre = "Carlos Mendoza",
                    aniosExperiencia = 8,
                    especialidades = listOf("Frenos", "Suspensión")
                ),
                MecanicoDisponible(
                    idMecanico = 2,
                    nombre = "Roberto Gómez",
                    aniosExperiencia = 5,
                    especialidades = listOf("Motor", "Diagnóstico Scanner")
                ),
                MecanicoDisponible(
                    idMecanico = 3,
                    nombre = "Ana Martínez",
                    aniosExperiencia = 6,
                    especialidades = listOf("Sistema Eléctrico", "Aire Acondicionado")
                )
            )
        )
    }

    override suspend fun turnos(idServicio: Long, fecha: String, idMecanico: Int): Result<List<Turno>> {
        return Result.success(
            listOf(
                Turno(hora = "08:00", disponible = true),
                Turno(hora = "09:00", disponible = false),
                Turno(hora = "10:00", disponible = true),
                Turno(hora = "11:00", disponible = true),
                Turno(hora = "14:00", disponible = false),
                Turno(hora = "15:00", disponible = true),
                Turno(hora = "16:00", disponible = true)
            )
        )
    }
}