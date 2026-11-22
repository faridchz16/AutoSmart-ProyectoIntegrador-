package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.dto.ConteoEstado;
import com.tecsup.autosmart.dto.DuracionOrden;
import com.tecsup.autosmart.dto.MontoMetodo;
import com.tecsup.autosmart.dto.ResumenCalificaciones;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoPago;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MetricasRepository extends Repository<Cita, Long> {

    @Query("SELECT new com.tecsup.autosmart.dto.ConteoEstado(c.estado, COUNT(c)) FROM Cita c "
            + "WHERE c.fecha >= :desde AND c.fecha <= :hasta GROUP BY c.estado")
    List<ConteoEstado> citasPorEstado(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT new com.tecsup.autosmart.dto.MontoMetodo(m.codigo, SUM(p.monto)) FROM Pago p JOIN p.metodo m "
            + "WHERE p.estado = :estado AND p.fecha >= :desde AND p.fecha < :hasta GROUP BY m.codigo")
    List<MontoMetodo> ingresosPorMetodo(@Param("estado") EstadoPago estado,
                                        @Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);

    @Query("SELECT new com.tecsup.autosmart.dto.DuracionOrden(o.fechaInicio, o.fechaFin) FROM OrdenTrabajo o "
            + "WHERE o.fechaFin >= :desde AND o.fechaFin < :hasta")
    List<DuracionOrden> ordenesTerminadas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT new com.tecsup.autosmart.dto.ResumenCalificaciones(AVG(c.estrellas), COUNT(c)) FROM Calificacion c "
            + "WHERE c.fecha >= :desde AND c.fecha < :hasta")
    ResumenCalificaciones calificaciones(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}