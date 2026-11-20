package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.EstadoPago;
import com.tecsup.autosmart.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    boolean existsByOrden_IdOrdenAndEstado(Long idOrden, EstadoPago estado);

    Optional<Pago> findFirstByOrden_IdOrdenAndEstado(Long idOrden, EstadoPago estado);
}