package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdMaterialNot(String codigo, Long idMaterial);
    List<Material> findAllByOrderByNombreAsc();

    @Query("SELECT m FROM Material m WHERE LOWER(m.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', :texto, '%')) ORDER BY m.nombre ASC")
    List<Material> buscar(@Param("texto") String texto);
}