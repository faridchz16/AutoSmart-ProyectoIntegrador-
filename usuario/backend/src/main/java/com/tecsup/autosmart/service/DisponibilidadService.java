package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.MecanicoDisponibleResponse;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Especialidad;
import com.tecsup.autosmart.model.PerfilMecanico;
import com.tecsup.autosmart.model.Servicio;
import com.tecsup.autosmart.repository.PerfilMecanicoRepository;
import com.tecsup.autosmart.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisponibilidadService {

    private final ServicioRepository servicioRepository;
    private final PerfilMecanicoRepository perfilRepository;

    public DisponibilidadService(ServicioRepository servicioRepository,
                                 PerfilMecanicoRepository perfilRepository) {
        this.servicioRepository = servicioRepository;
        this.perfilRepository = perfilRepository;
    }

    // Mecanicos activos que pueden atender el servicio (filtrados por su especialidad, si tiene)
    @Transactional(readOnly = true)
    public List<MecanicoDisponibleResponse> mecanicosPorServicio(Long idServicio) {
        Servicio servicio = servicioRepository.findByIdServicioAndActivoTrue(idServicio)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        List<PerfilMecanico> perfiles = servicio.getEspecialidad() == null
                ? perfilRepository.buscarActivos()
                : perfilRepository.buscarActivosPorEspecialidad(servicio.getEspecialidad());

        return perfiles.stream()
                .map(this::mapear)
                .sorted(Comparator.comparing(MecanicoDisponibleResponse::getNombre))
                .collect(Collectors.toList());
    }

    private MecanicoDisponibleResponse mapear(PerfilMecanico p) {
        MecanicoDisponibleResponse res = new MecanicoDisponibleResponse();
        res.setIdMecanico(p.getUsuario().getIdUsuario());
        res.setNombre(p.getUsuario().getNombre());
        res.setAniosExperiencia(p.getAniosExperiencia());
        res.setEspecialidades(p.getEspecialidades().stream()
                .map(Especialidad::getNombre)
                .sorted()
                .collect(Collectors.toList()));
        return res;
    }
}