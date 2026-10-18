package com.tecsup.autosmart.repository;

import com.tecsup.autosmart.model.HorarioMecanico;
import com.tecsup.autosmart.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HorarioMecanicoRepository extends JpaRepository<HorarioMecanico, Integer> {

    List<HorarioMecanico> findByMecanicoOrderByDiaSemanaAscHoraInicioAsc(Usuario mecanico);

    Optional<HorarioMecanico> findByIdHorarioAndMecanico(Integer idHorario, Usuario mecanico);

    // Dos rangos se cruzan si inicio1 < fin2 y fin1 > inicio2 (8-12 y 12-18 NO se cruzan)
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM HorarioMecanico h " +
            "WHERE h.mecanico = :mecanico AND h.diaSemana = :dia " +
            "AND h.horaInicio < :fin AND h.horaFin > :inicio AND h.idHorario <> :excluirId")
    boolean haySolapamiento(@Param("mecanico") Usuario mecanico,
                            @Param("dia") Integer dia,
                            @Param("inicio") LocalTime inicio,
                            @Param("fin") LocalTime fin,
                            @Param("excluirId") Integer excluirId);
}