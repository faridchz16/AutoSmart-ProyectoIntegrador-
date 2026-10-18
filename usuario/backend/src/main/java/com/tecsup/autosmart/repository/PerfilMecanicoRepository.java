package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Especialidad;
import com.tecsup.autosmart.model.PerfilMecanico;
import com.tecsup.autosmart.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerfilMecanicoRepository extends JpaRepository<PerfilMecanico, Integer> {
    Optional<PerfilMecanico> findByUsuario(Usuario usuario);

    @Query("SELECT p FROM PerfilMecanico p JOIN FETCH p.usuario u WHERE u.activo = true")
    List<PerfilMecanico> buscarActivos();

    @Query("SELECT p FROM PerfilMecanico p JOIN FETCH p.usuario u " +
            "WHERE u.activo = true AND :especialidad MEMBER OF p.especialidades")
    List<PerfilMecanico> buscarActivosPorEspecialidad(@Param("especialidad") Especialidad especialidad);
}