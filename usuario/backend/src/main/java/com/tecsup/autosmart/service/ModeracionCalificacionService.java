package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CalificacionAdminResponse;
import com.tecsup.autosmart.dto.OcultarCalificacionRequest;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.mapper.CalificacionMapper;
import com.tecsup.autosmart.model.Calificacion;
import com.tecsup.autosmart.repository.CalificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModeracionCalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final CalificacionMapper mapper;
    private final Clock clock;

    public ModeracionCalificacionService(CalificacionRepository calificacionRepository,
                                         CalificacionMapper mapper,
                                         Clock clock) {
        this.calificacionRepository = calificacionRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CalificacionAdminResponse> listar(Boolean oculto) {
        return calificacionRepository.buscarAdmin(oculto).stream()
                .map(mapper::toAdmin)
                .toList();
    }

    @Transactional
    public CalificacionAdminResponse ocultar(String correoAdmin, Long idCalificacion, OcultarCalificacionRequest request) {
        Calificacion calificacion = calificacionRepository.findById(idCalificacion)
                .orElseThrow(() -> new NotFoundException("Calificación no encontrada"));

        if (Boolean.TRUE.equals(calificacion.getOculto())) {
            throw new ConflictException("La calificación ya está oculta");
        }

        calificacion.setOculto(true);
        calificacion.setMotivoOcultado(request.motivo().trim());
        calificacion.setOcultadoPor(correoAdmin);
        calificacion.setFechaOcultado(LocalDateTime.now(clock).withNano(0));

        return mapper.toAdmin(calificacionRepository.save(calificacion));
    }
}