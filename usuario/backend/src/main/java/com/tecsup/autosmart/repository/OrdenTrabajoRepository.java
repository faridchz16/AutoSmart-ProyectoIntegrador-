package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {
    boolean existsByCita_IdCita(Long idCita);
    Optional<OrdenTrabajo> findByCita_IdCita(Long idCita);

    @Query("SELECT o FROM OrdenTrabajo o JOIN FETCH o.cita c JOIN FETCH c.servicio JOIN FETCH c.vehiculo " +
            "WHERE c.mecanico.correo = :correo AND c.estado IN :estados ORDER BY o.fechaInicio DESC")
    List<OrdenTrabajo> buscarDelMecanico(@Param("correo") String correo,
                                         @Param("estados") List<EstadoCita> estados);
}