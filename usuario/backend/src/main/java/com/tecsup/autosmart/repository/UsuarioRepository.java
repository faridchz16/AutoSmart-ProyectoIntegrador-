package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    @Query("SELECT u FROM Usuario u WHERE (:rol = '' OR u.rol.nombre = :rol) " +
            "AND (:q = '' OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Usuario> buscar(@Param("rol") String rol, @Param("q") String q, Pageable pageable);

    @Query(value = "SELECT * FROM usuarios WHERE id_usuario = :id FOR UPDATE", nativeQuery = true)
    Optional<Usuario> buscarParaReserva(@Param("id") Integer id);
}