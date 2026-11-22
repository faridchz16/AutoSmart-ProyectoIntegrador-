package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.EstadoPago;
import com.tecsup.autosmart.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    boolean existsByOrden_IdOrdenAndEstado(Long idOrden, EstadoPago estado);

    Optional<Pago> findFirstByOrden_IdOrdenAndEstado(Long idOrden, EstadoPago estado);

    @Query("SELECT p FROM Pago p JOIN FETCH p.metodo m JOIN FETCH p.orden o JOIN FETCH o.cita c "
            + "JOIN FETCH c.cliente JOIN FETCH c.vehiculo "
            + "WHERE p.fecha >= :desde AND p.fecha < :hasta "
            + "AND (:metodo IS NULL OR m.codigo = :metodo) "
            + "AND (:estado IS NULL OR p.estado = :estado) "
            + "ORDER BY p.fecha DESC")
    List<Pago> buscarAdmin(@Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta,
                           @Param("metodo") String metodo,
                           @Param("estado") EstadoPago estado);
}