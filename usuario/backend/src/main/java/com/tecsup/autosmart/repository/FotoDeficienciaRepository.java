package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.FotoDeficiencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FotoDeficienciaRepository extends JpaRepository<FotoDeficiencia, Long> {
    long countByDeficiencia_IdDeficiencia(Long idDeficiencia);
    List<FotoDeficiencia> findByDeficiencia_IdDeficienciaOrderByIdFotoAsc(Long idDeficiencia);
}