package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.FechaBloqueada;
import com.tecsup.autosmart.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FechaBloqueadaRepository extends JpaRepository<FechaBloqueada, Integer> {

    List<FechaBloqueada> findByMecanicoAndFechaGreaterThanEqualOrderByFechaAsc(Usuario mecanico, LocalDate desde);

    boolean existsByMecanicoAndFecha(Usuario mecanico, LocalDate fecha);

    Optional<FechaBloqueada> findByIdBloqueoAndMecanico(Integer idBloqueo, Usuario mecanico);
}