package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {
    boolean existsByCita_IdCita(Long idCita);
    Optional<OrdenTrabajo> findByCita_IdCita(Long idCita);
}