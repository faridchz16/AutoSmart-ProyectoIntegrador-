package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FeriadoRepository extends JpaRepository<Feriado, Integer> {

    boolean existsByFecha(LocalDate fecha);

    List<Feriado> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
}