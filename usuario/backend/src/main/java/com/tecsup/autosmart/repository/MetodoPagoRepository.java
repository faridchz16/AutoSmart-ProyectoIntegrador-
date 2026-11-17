package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Integer> {

    List<MetodoPago> findByHabilitadoTrueOrderByOrdenAsc();

    Optional<MetodoPago> findByCodigoAndHabilitadoTrue(String codigo);
}