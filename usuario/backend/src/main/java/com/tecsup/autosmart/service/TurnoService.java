package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.TurnoResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.Especialidad;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.HorarioMecanico;
import com.tecsup.autosmart.model.PerfilMecanico;
import com.tecsup.autosmart.model.Servicio;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.FechaBloqueadaRepository;
import com.tecsup.autosmart.repository.HorarioMecanicoRepository;
import com.tecsup.autosmart.repository.PerfilMecanicoRepository;
import com.tecsup.autosmart.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class TurnoService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final ServicioRepository servicioRepository;
    private final PerfilMecanicoRepository perfilRepository;
    private final HorarioMecanicoRepository horarioRepository;
    private final FechaBloqueadaRepository fechaBloqueadaRepository;
    private final CitaRepository citaRepository;

    public TurnoService(ServicioRepository servicioRepository,
                        PerfilMecanicoRepository perfilRepository,
                        HorarioMecanicoRepository horarioRepository,
                        FechaBloqueadaRepository fechaBloqueadaRepository,
                        CitaRepository citaRepository) {
        this.servicioRepository = servicioRepository;
        this.perfilRepository = perfilRepository;
        this.horarioRepository = horarioRepository;
        this.fechaBloqueadaRepository = fechaBloqueadaRepository;
        this.citaRepository = citaRepository;
    }

    // Turnos libres de un mecanico para un servicio en una fecha
    @Transactional(readOnly = true)
    public List<TurnoResponse> turnosLibres(Long idServicio, Integer idMecanico, LocalDate fecha) {
        Servicio servicio = servicioRepository.findByIdServicioAndActivoTrue(idServicio)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        PerfilMecanico perfil = perfilRepository.buscarActivos().stream()
                .filter(p -> p.getUsuario().getIdUsuario().equals(idMecanico))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Mecanico no encontrado"));

        Especialidad requerida = servicio.getEspecialidad();
        if (requerida != null) {
            boolean laTiene = perfil.getEspecialidades().stream()
                    .anyMatch(e -> e.getNombre().equalsIgnoreCase(requerida.getNombre()));
            if (!laTiene) {
                throw new BadRequestException("El mecanico no tiene la especialidad requerida por el servicio");
            }
        }

        LocalDate hoy = LocalDate.now(LIMA);
        if (fecha.isBefore(hoy)) {
            throw new BadRequestException("La fecha no puede ser pasada");
        }

        Usuario mecanico = perfil.getUsuario();

        if (fechaBloqueadaRepository.existsByMecanicoAndFecha(mecanico, fecha)) {
            return List.of();
        }

        List<Cita> ocupadas = citaRepository.buscarActivasDelDia(
                mecanico, fecha, List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA));

        LocalTime ahora = LocalTime.now(LIMA);
        int duracion = servicio.getDuracionMinutos();
        List<TurnoResponse> libres = new ArrayList<>();

        List<HorarioMecanico> horarios = horarioRepository
                .findByMecanicoAndDiaSemanaOrderByHoraInicioAsc(mecanico, fecha.getDayOfWeek().getValue());

        for (HorarioMecanico h : horarios) {
            LocalTime inicio = h.getHoraInicio();
            while (!inicio.plusMinutes(duracion).isAfter(h.getHoraFin())) {
                final LocalTime ini = inicio;
                final LocalTime fin = inicio.plusMinutes(duracion);
                boolean yaPaso = fecha.equals(hoy) && !ini.isAfter(ahora);
                boolean seCruza = ocupadas.stream()
                        .anyMatch(c -> ini.isBefore(c.getHoraFin()) && fin.isAfter(c.getHoraInicio()));
                if (!yaPaso && !seCruza) {
                    libres.add(new TurnoResponse(ini, fin));
                }
                inicio = fin;
            }
        }
        return libres;
    }
}