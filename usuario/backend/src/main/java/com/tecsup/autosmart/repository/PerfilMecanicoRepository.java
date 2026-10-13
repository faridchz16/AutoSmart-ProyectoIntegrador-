package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.PerfilMecanico;
import com.tecsup.autosmart.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilMecanicoRepository extends JpaRepository<PerfilMecanico, Integer> {
    Optional<PerfilMecanico> findByUsuario(Usuario usuario);
}