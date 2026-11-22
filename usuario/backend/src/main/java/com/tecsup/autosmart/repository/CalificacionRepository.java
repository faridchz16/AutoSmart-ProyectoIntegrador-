package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.dto.PromedioMecanico;
import com.tecsup.autosmart.model.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    boolean existsByOrden_IdOrden(Long idOrden);

    @Query("SELECT new com.tecsup.autosmart.dto.PromedioMecanico(c.mecanico.idUsuario, AVG(c.estrellas), COUNT(c)) "
            + "FROM Calificacion c GROUP BY c.mecanico.idUsuario")
    List<PromedioMecanico> promediosPorMecanico();

    @Query("SELECT c FROM Calificacion c JOIN FETCH c.orden o JOIN FETCH o.cita JOIN FETCH c.cliente "
            + "WHERE c.mecanico.correo = :correo ORDER BY c.fecha DESC")
    List<Calificacion> delMecanico(@Param("correo") String correo);

    @Query("SELECT c FROM Calificacion c JOIN FETCH c.orden o JOIN FETCH o.cita JOIN FETCH c.cliente JOIN FETCH c.mecanico "
            + "WHERE (:oculto IS NULL OR c.oculto = :oculto) ORDER BY c.fecha DESC")
    List<Calificacion> buscarAdmin(@Param("oculto") Boolean oculto);
}