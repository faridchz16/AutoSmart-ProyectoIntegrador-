package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.EstadoSolicitud;
import com.tecsup.autosmart.model.SolicitudMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudMaterialRepository extends JpaRepository<SolicitudMaterial, Long> {
    List<SolicitudMaterial> findByOrden_IdOrdenOrderByIdSolicitudAsc(Long idOrden);
    boolean existsByOrden_IdOrdenAndEstado(Long idOrden, EstadoSolicitud estado);
}