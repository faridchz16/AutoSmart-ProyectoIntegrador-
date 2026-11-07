package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.TareaEstadoRequest;
import com.tecsup.autosmart.dto.TareaRequest;
import com.tecsup.autosmart.dto.TareasOrdenResponse;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.TareaOrden;
import com.tecsup.autosmart.repository.TareaOrdenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TareaOrdenService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final TareaOrdenRepository tareaRepository;
    private final OrdenTrabajoService ordenService;

    public TareaOrdenService(TareaOrdenRepository tareaRepository, OrdenTrabajoService ordenService) {
        this.tareaRepository = tareaRepository;
        this.ordenService = ordenService;
    }

    @Transactional
    public TareasOrdenResponse agregar(String correoMecanico, Long idOrden, TareaRequest req) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        ordenService.validarEnProceso(orden);

        TareaOrden tarea = new TareaOrden();
        tarea.setOrden(orden);
        tarea.setDescripcion(req.getDescripcion().trim());
        tarea.setFechaCreacion(LocalDateTime.now(ZONA_LIMA).withNano(0));
        tareaRepository.save(tarea);

        return resumen(idOrden);
    }

    @Transactional
    public TareasOrdenResponse cambiarEstado(String correoMecanico, Long idOrden, Long idTarea, TareaEstadoRequest req) {
        OrdenTrabajo orden = ordenService.ordenDelMecanico(correoMecanico, idOrden);
        ordenService.validarEnProceso(orden);

        TareaOrden tarea = tareaRepository.findByIdTareaAndOrden_IdOrden(idTarea, idOrden)
                .orElseThrow(() -> new NotFoundException("Tarea no encontrada en esta orden"));
        tarea.setCompletada(req.getCompletada());
        tarea.setFechaCompletada(req.getCompletada() ? LocalDateTime.now(ZONA_LIMA).withNano(0) : null);
        tareaRepository.save(tarea);

        return resumen(idOrden);
    }

    @Transactional(readOnly = true)
    public TareasOrdenResponse listar(String correoMecanico, Long idOrden) {
        ordenService.ordenDelMecanico(correoMecanico, idOrden);
        return resumen(idOrden);
    }

    @Transactional(readOnly = true)
    public TareasOrdenResponse resumen(Long idOrden) {
        List<TareaOrden> tareas = tareaRepository.findByOrden_IdOrdenOrderByIdTareaAsc(idOrden);
        int total = tareas.size();
        int completadas = (int) tareas.stream().filter(TareaOrden::isCompletada).count();

        TareasOrdenResponse res = new TareasOrdenResponse();
        res.setIdOrden(idOrden);
        res.setTotalTareas(total);
        res.setCompletadas(completadas);
        res.setPorcentajeAvance(total == 0 ? 0 : Math.round(completadas * 100f / total));
        res.setTareas(tareas.stream()
                .map(t -> new TareasOrdenResponse.Tarea(
                        t.getIdTarea(),
                        t.getDescripcion(),
                        t.isCompletada(),
                        t.getFechaCreacion().toString(),
                        t.getFechaCompletada() == null ? null : t.getFechaCompletada().toString()))
                .collect(Collectors.toList()));
        return res;
    }
}