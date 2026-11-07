package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.TareaOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TareaOrdenRepository extends JpaRepository<TareaOrden, Long> {
    List<TareaOrden> findByOrden_IdOrdenOrderByIdTareaAsc(Long idOrden);
    Optional<TareaOrden> findByIdTareaAndOrden_IdOrden(Long idTarea, Long idOrden);
}