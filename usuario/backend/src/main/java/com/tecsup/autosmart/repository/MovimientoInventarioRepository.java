package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findByMaterial_IdMaterialOrderByIdMovimientoAsc(Long idMaterial);
}