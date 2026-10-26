package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.BloqueoTurno;
import com.tecsup.autosmart.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BloqueoTurnoRepository extends JpaRepository<BloqueoTurno, Long> {

    @Query("SELECT b FROM BloqueoTurno b WHERE b.mecanico = :mecanico AND b.fecha = :fecha " +
            "AND b.expiraEn > :ahora AND (:idCliente = 0 OR b.cliente.idUsuario <> :idCliente)")
    List<BloqueoTurno> buscarVigentes(@Param("mecanico") Usuario mecanico,
                                      @Param("fecha") LocalDate fecha,
                                      @Param("ahora") LocalDateTime ahora,
                                      @Param("idCliente") int idCliente);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM BloqueoTurno b WHERE b.cliente = :cliente")
    void liberarDelCliente(@Param("cliente") Usuario cliente);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM BloqueoTurno b WHERE b.expiraEn <= :ahora")
    void borrarVencidos(@Param("ahora") LocalDateTime ahora);
}