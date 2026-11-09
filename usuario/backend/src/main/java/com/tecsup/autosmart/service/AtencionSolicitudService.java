package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.DecisionSolicitudRequest;
import com.tecsup.autosmart.dto.SolicitudMaterialResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.EstadoSolicitud;
import com.tecsup.autosmart.model.SolicitudMaterial;
import com.tecsup.autosmart.repository.SolicitudMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AtencionSolicitudService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final SolicitudMaterialRepository solicitudRepository;
    private final SolicitudMaterialService solicitudService;
    private final KardexService kardexService;

    public AtencionSolicitudService(SolicitudMaterialRepository solicitudRepository,
                                    SolicitudMaterialService solicitudService,
                                    KardexService kardexService) {
        this.solicitudRepository = solicitudRepository;
        this.solicitudService = solicitudService;
        this.kardexService = kardexService;
    }

    @Transactional(readOnly = true)
    public List<SolicitudMaterialResponse> listar(EstadoSolicitud estado) {
        List<SolicitudMaterial> solicitudes = (estado == null)
                ? solicitudRepository.findAllByOrderByFechaSolicitudDesc()
                : solicitudRepository.findByEstadoOrderByFechaSolicitudAsc(estado);
        return solicitudes.stream()
                .map(solicitudService::aRespuesta)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public SolicitudMaterialResponse decidir(String correoAdmin, Long idSolicitud, DecisionSolicitudRequest req) {
        if (req.getDecision() == EstadoSolicitud.PENDIENTE) {
            throw new BadRequestException("La decision debe ser APROBADA o RECHAZADA");
        }
        SolicitudMaterial solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new NotFoundException("Solicitud no encontrada"));
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ConflictException("La solicitud ya fue " + solicitud.getEstado());
        }

        if (req.getDecision() == EstadoSolicitud.APROBADA) {
            EstadoCita estadoOrden = solicitud.getOrden().getCita().getEstado();
            if (estadoOrden != EstadoCita.EN_PROCESO && estadoOrden != EstadoCita.ESPERA_APROBACION) {
                throw new ConflictException("La orden de la solicitud ya no esta activa (estado: " + estadoOrden + ")");
            }
            kardexService.registrarSalida(solicitud.getOrden(), solicitud.getMaterial().getIdMaterial(),
                    solicitud.getCantidad(), correoAdmin, "Solicitud de repuesto #" + solicitud.getIdSolicitud());
        }

        solicitud.setEstado(req.getDecision());
        solicitud.setAtendidoPor(correoAdmin);
        solicitud.setFechaAtencion(LocalDateTime.now(ZONA_LIMA).withNano(0));
        solicitud.setObservacion(req.getObservacion() == null || req.getObservacion().isBlank()
                ? null : req.getObservacion().trim());

        return solicitudService.aRespuesta(solicitudRepository.save(solicitud));
    }
}