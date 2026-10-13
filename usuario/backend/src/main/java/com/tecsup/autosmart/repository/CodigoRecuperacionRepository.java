package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.CodigoRecuperacion;
import com.tecsup.autosmart.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {
    List<CodigoRecuperacion> findByUsuarioAndUsadoFalse(Usuario usuario);
    Optional<CodigoRecuperacion> findFirstByUsuarioAndUsadoFalseOrderByIdCodigoDesc(Usuario usuario);
}