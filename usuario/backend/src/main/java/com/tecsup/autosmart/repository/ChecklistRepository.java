package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChecklistRepository extends JpaRepository<Checklist, Long> {
    Optional<Checklist> findByOrden_IdOrden(Long idOrden);
}