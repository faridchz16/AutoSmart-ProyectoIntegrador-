package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.dto.PromedioMecanico;
import com.tecsup.autosmart.model.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    boolean existsByOrden_IdOrden(Long idOrden);

    @Query("SELECT new com.tecsup.autosmart.dto.PromedioMecanico(c.mecanico.idUsuario, AVG(c.estrellas), COUNT(c)) "
            + "FROM Calificacion c GROUP BY c.mecanico.idUsuario")
    List<PromedioMecanico> promediosPorMecanico();
}