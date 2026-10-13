package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer> {
    Optional<Especialidad> findByNombreIgnoreCase(String nombre);
}