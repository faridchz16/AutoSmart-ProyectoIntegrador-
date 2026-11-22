package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.MecanicoDisponibleResponse;
import com.tecsup.autosmart.dto.PromedioMecanico;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Especialidad;
import com.tecsup.autosmart.model.PerfilMecanico;
import com.tecsup.autosmart.model.Servicio;
import com.tecsup.autosmart.repository.CalificacionRepository;
import com.tecsup.autosmart.repository.PerfilMecanicoRepository;
import com.tecsup.autosmart.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DisponibilidadService {

    private final ServicioRepository servicioRepository;
    private final PerfilMecanicoRepository perfilRepository;
    private final CalificacionRepository calificacionRepository;

    public DisponibilidadService(ServicioRepository servicioRepository,
                                 PerfilMecanicoRepository perfilRepository,
                                 CalificacionRepository calificacionRepository) {
        this.servicioRepository = servicioRepository;
        this.perfilRepository = perfilRepository;
        this.calificacionRepository = calificacionRepository;
    }

    @Transactional(readOnly = true)
    public List<MecanicoDisponibleResponse> mecanicosPorServicio(Long idServicio) {
        Servicio servicio = servicioRepository.findByIdServicioAndActivoTrue(idServicio)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        List<PerfilMecanico> perfiles = servicio.getEspecialidad() == null
                ? perfilRepository.buscarActivos()
                : perfilRepository.buscarActivosPorEspecialidad(servicio.getEspecialidad());

        Map<Integer, PromedioMecanico> promedios = calificacionRepository.promediosPorMecanico().stream()
                .collect(Collectors.toMap(PromedioMecanico::idMecanico, Function.identity()));

        return perfiles.stream()
                .map(p -> mapear(p, promedios.get(p.getUsuario().getIdUsuario())))
                .sorted(Comparator.comparing(MecanicoDisponibleResponse::getCalificacionPromedio,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MecanicoDisponibleResponse::getNombre))
                .collect(Collectors.toList());
    }

    private MecanicoDisponibleResponse mapear(PerfilMecanico p, PromedioMecanico promedio) {
        MecanicoDisponibleResponse res = new MecanicoDisponibleResponse();
        res.setIdMecanico(p.getUsuario().getIdUsuario());
        res.setNombre(p.getUsuario().getNombre());
        res.setAniosExperiencia(p.getAniosExperiencia());
        res.setEspecialidades(p.getEspecialidades().stream()
                .map(Especialidad::getNombre)
                .sorted()
                .collect(Collectors.toList()));
        if (promedio != null) {
            res.setCalificacionPromedio(Math.round(promedio.promedio() * 10) / 10.0);
            res.setTotalResenas(promedio.total());
        }
        return res;
    }
}