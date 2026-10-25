package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CitaAdminResponse;
import com.tecsup.autosmart.dto.MecanicoLibreResponse;
import com.tecsup.autosmart.dto.PaginaResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.Especialidad;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.PerfilMecanico;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.PerfilMecanicoRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminCitaService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilMecanicoRepository perfilRepository;
    private final TurnoService turnoService;

    public AdminCitaService(CitaRepository citaRepository,
                            UsuarioRepository usuarioRepository,
                            PerfilMecanicoRepository perfilRepository,
                            TurnoService turnoService) {
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.turnoService = turnoService;
    }

    // Reservas del taller con filtros combinados y paginacion (T38.1)
    @Transactional(readOnly = true)
    public PaginaResponse<CitaAdminResponse> listar(LocalDate desde, LocalDate hasta, String estado,
                                                    Integer idMecanico, String q, int page, int size) {
        if (page < 0) {
            throw new BadRequestException("La pagina no puede ser negativa");
        }
        if (size < 1 || size > 50) {
            throw new BadRequestException("El tamano debe estar entre 1 y 50");
        }

        LocalDate d = desde == null ? LocalDate.of(2000, 1, 1) : desde;
        LocalDate h = hasta == null ? LocalDate.of(2999, 12, 31) : hasta;
        if (d.isAfter(h)) {
            throw new BadRequestException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }

        boolean todosEstados = estado == null || estado.isBlank();
        EstadoCita est = EstadoCita.PENDIENTE;
        if (!todosEstados) {
            try {
                est = EstadoCita.valueOf(estado.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Estado invalido. Usa PENDIENTE, CONFIRMADA, RECHAZADA o CANCELADA");
            }
        }

        int mecanico = idMecanico == null ? 0 : idMecanico;
        String texto = q == null ? "" : q.trim();

        Page<Cita> resultado = citaRepository.buscarAdmin(d, h, todosEstados, est, mecanico, texto,
                PageRequest.of(page, size,
                        Sort.by(Sort.Order.desc("fecha"), Sort.Order.asc("horaInicio"))));

        PaginaResponse<CitaAdminResponse> res = new PaginaResponse<>();
        res.setContenido(resultado.getContent().stream().map(this::aAdmin).collect(Collectors.toList()));
        res.setPagina(resultado.getNumber());
        res.setTamano(resultado.getSize());
        res.setTotalElementos(resultado.getTotalElements());
        res.setTotalPaginas(resultado.getTotalPages());
        return res;
    }

    // Mecanicos que pueden tomar la cita (misma especialidad, libres en ese horario, sin el actual) (T39.1)
    @Transactional(readOnly = true)
    public List<MecanicoLibreResponse> mecanicosLibres(Long idCita) {
        Cita cita = citaReasignable(idCita);
        Especialidad requerida = cita.getServicio().getEspecialidad();

        List<PerfilMecanico> perfiles = requerida == null
                ? perfilRepository.buscarActivos()
                : perfilRepository.buscarActivosPorEspecialidad(requerida);

        return perfiles.stream()
                .filter(p -> !p.getUsuario().getIdUsuario().equals(cita.getMecanico().getIdUsuario()))
                .filter(p -> turnoService.mecanicoLibre(
                        p.getUsuario(), cita.getFecha(), cita.getHoraInicio(), cita.getHoraFin()))
                .map(this::aLibre)
                .sorted(Comparator.comparing(MecanicoLibreResponse::getNombre))
                .collect(Collectors.toList());
    }

    // Reasignar la cita a otro mecanico (T39.1)
    @Transactional
    public CitaAdminResponse reasignar(Long idCita, Integer idMecanico) {
        // Primero se bloquea al mecanico nuevo: evita que alguien le reserve ese turno mientras tanto
        Usuario nuevo = usuarioRepository.buscarParaReserva(idMecanico)
                .orElseThrow(() -> new NotFoundException("Mecanico no encontrado"));
        if (!Boolean.TRUE.equals(nuevo.getActivo()) || !"MECANICO".equals(nuevo.getRol().getNombre())) {
            throw new NotFoundException("Mecanico no encontrado");
        }

        Cita cita = citaReasignable(idCita);

        if (cita.getMecanico().getIdUsuario().equals(nuevo.getIdUsuario())) {
            throw new ConflictException("La cita ya esta asignada a ese mecanico");
        }

        Especialidad requerida = cita.getServicio().getEspecialidad();
        if (requerida != null) {
            boolean laTiene = perfilRepository.findByUsuario(nuevo)
                    .map(p -> p.getEspecialidades().stream()
                            .anyMatch(e -> e.getNombre().equalsIgnoreCase(requerida.getNombre())))
                    .orElse(false);
            if (!laTiene) {
                throw new BadRequestException("El mecanico no tiene la especialidad requerida por el servicio");
            }
        }

        if (!turnoService.mecanicoLibre(nuevo, cita.getFecha(), cita.getHoraInicio(), cita.getHoraFin())) {
            throw new ConflictException("El mecanico no esta libre en ese horario");
        }

        cita.setMecanico(nuevo);
        cita.setEstado(EstadoCita.PENDIENTE); // el mecanico nuevo debe aceptarla
        return aAdmin(citaRepository.save(cita));
    }

    private Cita citaReasignable(Long idCita) {
        Cita cita = citaRepository.buscarDetalle(idCita)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        if (cita.getEstado() == EstadoCita.CANCELADA || cita.getEstado() == EstadoCita.RECHAZADA) {
            throw new ConflictException("La cita ya esta cancelada o rechazada");
        }
        if (cita.getFecha().isBefore(LocalDate.now(LIMA))) {
            throw new ConflictException("La cita ya paso");
        }
        return cita;
    }

    private MecanicoLibreResponse aLibre(PerfilMecanico p) {
        MecanicoLibreResponse res = new MecanicoLibreResponse();
        res.setIdUsuario(p.getUsuario().getIdUsuario());
        res.setNombre(p.getUsuario().getNombre());
        res.setEspecialidades(p.getEspecialidades().stream()
                .map(Especialidad::getNombre)
                .sorted()
                .collect(Collectors.toList()));
        return res;
    }

    private CitaAdminResponse aAdmin(Cita c) {
        CitaAdminResponse res = new CitaAdminResponse();
        res.setIdCita(c.getIdCita());
        res.setTicket(c.getTicket());
        res.setFecha(c.getFecha().toString());
        res.setHoraInicio(c.getHoraInicio().toString());
        res.setHoraFin(c.getHoraFin().toString());
        res.setEstado(c.getEstado().name());
        res.setServicio(c.getServicio().getNombre());
        res.setPrecio(c.getPrecio());
        res.setCliente(c.getCliente().getNombre());
        res.setPlaca(c.getVehiculo().getPlaca());
        res.setIdMecanico(c.getMecanico().getIdUsuario());
        res.setMecanico(c.getMecanico().getNombre());
        return res;
    }
}