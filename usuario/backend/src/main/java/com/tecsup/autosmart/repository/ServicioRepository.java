package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
}