package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Deficiencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeficienciaRepository extends JpaRepository<Deficiencia, Long> {
    List<Deficiencia> findByOrden_IdOrdenOrderByIdDeficienciaAsc(Long idOrden);
}