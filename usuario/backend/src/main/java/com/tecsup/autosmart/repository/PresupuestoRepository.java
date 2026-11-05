package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {
    Optional<Presupuesto> findFirstByOrden_IdOrdenOrderByIdPresupuestoDesc(Long idOrden);
}