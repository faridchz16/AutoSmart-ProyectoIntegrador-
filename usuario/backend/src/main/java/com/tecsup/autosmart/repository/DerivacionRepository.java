package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Derivacion;
import com.tecsup.autosmart.model.EstadoDerivacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DerivacionRepository extends JpaRepository<Derivacion, Long> {

    Optional<Derivacion> findFirstByIdConversacionAndEstado(String idConversacion, EstadoDerivacion estado);

    @Query("SELECT d FROM Derivacion d LEFT JOIN FETCH d.usuario "
            + "WHERE (:estado IS NULL OR d.estado = :estado) ORDER BY d.fecha DESC")
    List<Derivacion> buscar(@Param("estado") EstadoDerivacion estado);
}