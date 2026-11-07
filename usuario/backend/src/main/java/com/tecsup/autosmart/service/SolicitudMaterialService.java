package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.SolicitudMaterialRequest;
import com.tecsup.autosmart.dto.SolicitudMaterialResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.model.Material;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.SolicitudMaterial;
import com.tecsup.autosmart.repository.SolicitudMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolicitudMaterialService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final SolicitudMaterialRepository solicitudRepository;
    private final OrdenTrabajoService ordenService;
    private final MaterialService materialService;

    public SolicitudMaterialService(SolicitudMaterialRepository solicitudRepository,
                                    OrdenTrabajoService ordenService,
                                    MaterialService materialService) {
        this.solicitudRepository = solicitudRepository;
        this.ordenService = ordenService;
        this.materialService = materialService;
    }

    @Transactional
    public SolicitudMaterialResponse solicitar(String correoMecanico, Long idOrden, SolicitudMaterialRequest req) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        ordenService.validarEnProceso(orden);

        Material material = materialService.buscarPorId(req.getIdMaterial());
        if (material.getStockActual() < req.getCantidad()) {
            throw new ConflictException("Stock insuficiente de " + material.getNombre()
                    + ": disponible " + material.getStockActual() + ", solicitado " + req.getCantidad());
        }

        SolicitudMaterial solicitud = new SolicitudMaterial();
        solicitud.setOrden(orden);
        solicitud.setMaterial(material);
        solicitud.setCantidad(req.getCantidad());
        solicitud.setMotivo(req.getMotivo() == null || req.getMotivo().isBlank() ? null : req.getMotivo().trim());
        solicitud.setSolicitadoPor(correoMecanico);
        solicitud.setFechaSolicitud(LocalDateTime.now(ZONA_LIMA).withNano(0));

        return aRespuesta(solicitudRepository.save(solicitud));
    }

    @Transactional(readOnly = true)
    public List<SolicitudMaterialResponse> listar(String correoMecanico, Long idOrden) {
        ordenService.ordenDelMecanico(correoMecanico, idOrden);
        return solicitudRepository.findByOrden_IdOrdenOrderByIdSolicitudAsc(idOrden).stream()
                .map(this::aRespuesta)
                .collect(Collectors.toList());
    }

    public SolicitudMaterialResponse aRespuesta(SolicitudMaterial s) {
        SolicitudMaterialResponse res = new SolicitudMaterialResponse();
        res.setIdSolicitud(s.getIdSolicitud());
        res.setIdOrden(s.getOrden().getIdOrden());
        res.setIdMaterial(s.getMaterial().getIdMaterial());
        res.setCodigoMaterial(s.getMaterial().getCodigo());
        res.setNombreMaterial(s.getMaterial().getNombre());
        res.setCantidad(s.getCantidad());
        res.setStockDisponible(s.getMaterial().getStockActual());
        res.setMotivo(s.getMotivo());
        res.setEstado(s.getEstado().name());
        res.setSolicitadoPor(s.getSolicitadoPor());
        res.setFechaSolicitud(s.getFechaSolicitud().toString());
        res.setAtendidoPor(s.getAtendidoPor());
        res.setFechaAtencion(s.getFechaAtencion() == null ? null : s.getFechaAtencion().toString());
        res.setObservacion(s.getObservacion());
        return res;
    }
}