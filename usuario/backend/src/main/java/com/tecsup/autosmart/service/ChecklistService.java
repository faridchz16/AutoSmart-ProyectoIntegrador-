package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.ChecklistRequest;
import com.tecsup.autosmart.dto.ChecklistResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Checklist;
import com.tecsup.autosmart.model.ChecklistItem;
import com.tecsup.autosmart.model.ElementoChecklist;
import com.tecsup.autosmart.model.EstadoItemChecklist;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.repository.ChecklistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChecklistService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final ChecklistRepository checklistRepository;
    private final OrdenTrabajoService ordenService;

    public ChecklistService(ChecklistRepository checklistRepository, OrdenTrabajoService ordenService) {
        this.checklistRepository = checklistRepository;
        this.ordenService = ordenService;
    }

    @Transactional(readOnly = true)
    public ChecklistResponse obtener(String correoMecanico, Long idOrden) {
        ordenService.ordenDelMecanico(correoMecanico, idOrden);
        Checklist checklist = checklistRepository.findByOrden_IdOrden(idOrden)
                .orElseThrow(() -> new NotFoundException("La orden aun no tiene checklist"));
        return aRespuesta(checklist);
    }

    @Transactional
    public ChecklistResponse guardar(String correoMecanico, Long idOrden, ChecklistRequest req) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        ordenService.validarEnProceso(orden);

        Map<ElementoChecklist, ChecklistRequest.Item> porElemento = new EnumMap<>(ElementoChecklist.class);
        for (ChecklistRequest.Item item : req.getItems()) {
            if (porElemento.put(item.getElemento(), item) != null) {
                throw new BadRequestException("El elemento " + item.getElemento() + " esta repetido");
            }
            if (item.getEstado() == EstadoItemChecklist.OBSERVADO
                    && (item.getObservacion() == null || item.getObservacion().isBlank())) {
                throw new BadRequestException("El elemento " + item.getElemento() + " esta OBSERVADO y necesita una observacion");
            }
        }
        if (porElemento.size() != ElementoChecklist.values().length) {
            throw new BadRequestException("El checklist debe tener los 5 elementos: LUCES, FRENOS, NEUMATICOS, FLUIDOS y CARROCERIA");
        }

        Checklist checklist = checklistRepository.findByOrden_IdOrden(idOrden).orElseGet(() -> {
            Checklist nuevo = new Checklist();
            nuevo.setOrden(orden);
            return nuevo;
        });
        checklist.setFechaRegistro(LocalDateTime.now(ZONA_LIMA).withNano(0));
        checklist.setObservacionGeneral(limpiar(req.getObservacionGeneral()));

        for (ElementoChecklist elemento : ElementoChecklist.values()) {
            ChecklistRequest.Item datos = porElemento.get(elemento);
            ChecklistItem item = checklist.getItems().stream()
                    .filter(i -> i.getElemento() == elemento)
                    .findFirst()
                    .orElseGet(() -> {
                        ChecklistItem nuevo = new ChecklistItem();
                        nuevo.setChecklist(checklist);
                        nuevo.setElemento(elemento);
                        checklist.getItems().add(nuevo);
                        return nuevo;
                    });
            item.setEstado(datos.getEstado());
            item.setObservacion(limpiar(datos.getObservacion()));
        }

        return aRespuesta(checklistRepository.save(checklist));
    }

    private String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    private ChecklistResponse aRespuesta(Checklist c) {
        ChecklistResponse res = new ChecklistResponse();
        res.setIdChecklist(c.getIdChecklist());
        res.setIdOrden(c.getOrden().getIdOrden());
        res.setFechaRegistro(c.getFechaRegistro().toString());
        res.setObservacionGeneral(c.getObservacionGeneral());
        res.setItems(c.getItems().stream()
                .sorted(Comparator.comparing(ChecklistItem::getElemento))
                .map(i -> new ChecklistResponse.Item(i.getElemento().name(), i.getEstado().name(), i.getObservacion()))
                .collect(Collectors.toList()));
        return res;
    }
}