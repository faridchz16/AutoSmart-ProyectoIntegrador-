package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdServicioNot(String nombre, Long idServicio);

    List<Servicio> findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);
    Optional<Servicio> findByIdServicioAndActivoTrue(Long idServicio);
}