package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.OrdenHistorialMecanicoResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.mapper.HistorialMecanicoMapper;
import com.tecsup.autosmart.model.Calificacion;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.repository.CalificacionRepository;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HistorialMecanicoService {

    private static final List<EstadoCita> TERMINADAS = List.of(EstadoCita.FINALIZADA, EstadoCita.PAGADO);
    private static final LocalDate INICIO_SISTEMA = LocalDate.of(2000, 1, 1);

    private final OrdenTrabajoRepository ordenRepository;
    private final CalificacionRepository calificacionRepository;
    private final HistorialMecanicoMapper mapper;
    private final Clock clock;

    public HistorialMecanicoService(OrdenTrabajoRepository ordenRepository,
                                    CalificacionRepository calificacionRepository,
                                    HistorialMecanicoMapper mapper,
                                    Clock clock) {
        this.ordenRepository = ordenRepository;
        this.calificacionRepository = calificacionRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<OrdenHistorialMecanicoResponse> historial(String correoMecanico, LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : INICIO_SISTEMA;
        LocalDate fin = hasta != null ? hasta : LocalDate.now(clock);
        if (inicio.isAfter(fin)) {
            throw new BadRequestException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }

        Map<Long, Integer> estrellasPorOrden = calificacionRepository.delMecanico(correoMecanico).stream()
                .collect(Collectors.toMap(c -> c.getOrden().getIdOrden(), Calificacion::getEstrellas));

        return ordenRepository.historialDelMecanico(correoMecanico, TERMINADAS,
                        inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay()).stream()
                .map(o -> mapper.toResponse(o, estrellasPorOrden.get(o.getIdOrden())))
                .toList();
    }
}