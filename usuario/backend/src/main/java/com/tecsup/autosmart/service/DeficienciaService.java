package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.DeficienciaRequest;
import com.tecsup.autosmart.dto.DeficienciaResponse;
import com.tecsup.autosmart.model.Deficiencia;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.repository.DeficienciaRepository;
import com.tecsup.autosmart.repository.FotoDeficienciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeficienciaService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final DeficienciaRepository deficienciaRepository;
    private final OrdenTrabajoService ordenService;
    private final FotoDeficienciaRepository fotoRepository;

    public DeficienciaService(DeficienciaRepository deficienciaRepository, OrdenTrabajoService ordenService,
                              FotoDeficienciaRepository fotoRepository) {
        this.deficienciaRepository = deficienciaRepository;
        this.ordenService = ordenService;
        this.fotoRepository = fotoRepository;
    }

    @Transactional
    public DeficienciaResponse registrar(String correoMecanico, Long idOrden, DeficienciaRequest req) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        ordenService.validarEnProceso(orden);

        Deficiencia deficiencia = new Deficiencia();
        deficiencia.setOrden(orden);
        deficiencia.setCategoria(req.getCategoria());
        deficiencia.setSeveridad(req.getSeveridad());
        deficiencia.setDescripcion(req.getDescripcion().trim());
        deficiencia.setFechaRegistro(LocalDateTime.now(ZONA_LIMA).withNano(0));

        return aRespuesta(deficienciaRepository.save(deficiencia));
    }

    @Transactional(readOnly = true)
    public List<DeficienciaResponse> listar(String correoMecanico, Long idOrden) {
        ordenService.ordenDelMecanico(correoMecanico, idOrden);
        return deficienciaRepository.findByOrden_IdOrdenOrderByIdDeficienciaAsc(idOrden).stream()
                .map(this::aRespuesta)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeficienciaResponse> listarPorOrden(Long idOrden) {
        return deficienciaRepository.findByOrden_IdOrdenOrderByIdDeficienciaAsc(idOrden).stream()
                .map(this::aRespuesta)
                .collect(Collectors.toList());
    }

    private DeficienciaResponse aRespuesta(Deficiencia d) {
        DeficienciaResponse res = new DeficienciaResponse();
        res.setIdDeficiencia(d.getIdDeficiencia());
        res.setIdOrden(d.getOrden().getIdOrden());
        res.setCategoria(d.getCategoria().name());
        res.setSeveridad(d.getSeveridad().name());
        res.setDescripcion(d.getDescripcion());
        res.setFechaRegistro(d.getFechaRegistro().toString());

        res.setFotos(fotoRepository.findByDeficiencia_IdDeficienciaOrderByIdFotoAsc(d.getIdDeficiencia()).stream()
                .map(f -> new DeficienciaResponse.Foto(f.getIdFoto(), "/photos/" + f.getIdFoto()))
                .collect(Collectors.toList()));
        return res;
    }
}