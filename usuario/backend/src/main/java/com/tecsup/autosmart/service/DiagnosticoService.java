package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.DiagnosticoRequest;
import com.tecsup.autosmart.dto.DiagnosticoResponse;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class DiagnosticoService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final OrdenTrabajoRepository ordenRepository;
    private final OrdenTrabajoService ordenService;

    public DiagnosticoService(OrdenTrabajoRepository ordenRepository, OrdenTrabajoService ordenService) {
        this.ordenRepository = ordenRepository;
        this.ordenService = ordenService;
    }

    @Transactional
    public DiagnosticoResponse registrar(String correoMecanico, Long idOrden, DiagnosticoRequest req) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        ordenService.validarEnProceso(orden);

        orden.setDiagnostico(req.getDiagnostico().trim());
        orden.setFechaDiagnostico(LocalDateTime.now(ZONA_LIMA).withNano(0));

        return aRespuesta(ordenRepository.save(orden));
    }

    @Transactional(readOnly = true)
    public DiagnosticoResponse obtener(String correoMecanico, Long idOrden) {
        return aRespuesta(ordenService.ordenDelMecanico(correoMecanico, idOrden));
    }

    private DiagnosticoResponse aRespuesta(OrdenTrabajo o) {
        return new DiagnosticoResponse(
                o.getIdOrden(),
                o.getDiagnostico(),
                o.getFechaDiagnostico() == null ? null : o.getFechaDiagnostico().toString());
    }
}