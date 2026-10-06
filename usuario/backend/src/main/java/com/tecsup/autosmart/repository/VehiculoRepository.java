package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    boolean existsByPlaca(String placa);
    Optional<Vehiculo> findByPlaca(String placa);
    List<Vehiculo> findByCliente(com.tecsup.autosmart.model.Usuario cliente);
}