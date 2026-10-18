package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Query("SELECT c FROM Cita c " +
            "JOIN FETCH c.cliente JOIN FETCH c.servicio JOIN FETCH c.vehiculo " +
            "WHERE c.mecanico = :mecanico AND c.fecha BETWEEN :desde AND :hasta " +
            "ORDER BY c.fecha ASC, c.horaInicio ASC")
    List<Cita> buscarAgenda(@Param("mecanico") Usuario mecanico,
                            @Param("desde") LocalDate desde,
                            @Param("hasta") LocalDate hasta);

    boolean existsByMecanicoAndFechaAndEstado(Usuario mecanico, LocalDate fecha, EstadoCita estado);
}