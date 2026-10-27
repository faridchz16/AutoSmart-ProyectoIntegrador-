package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.BloqueoRequest;
import com.tecsup.autosmart.dto.BloqueoResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.BloqueoTurno;
import com.tecsup.autosmart.model.Servicio;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.BloqueoTurnoRepository;
import com.tecsup.autosmart.repository.ServicioRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class BloqueoTurnoService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final int MINUTOS_BLOQUEO = 10;

    private final BloqueoTurnoRepository bloqueoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final TurnoService turnoService;

    public BloqueoTurnoService(BloqueoTurnoRepository bloqueoRepository,
                               UsuarioRepository usuarioRepository,
                               ServicioRepository servicioRepository,
                               TurnoService turnoService) {
        this.bloqueoRepository = bloqueoRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioRepository = servicioRepository;
        this.turnoService = turnoService;
    }

    @Transactional
    public BloqueoResponse bloquear(String correoCliente, BloqueoRequest req) {

        Usuario mecanico = usuarioRepository.buscarParaReserva(req.getIdMecanico())
                .orElseThrow(() -> new NotFoundException("Mecanico no encontrado"));
        Usuario cliente = usuarioRepository.findByCorreo(correoCliente)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        Servicio servicio = servicioRepository.findByIdServicioAndActivoTrue(req.getIdServicio())
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        LocalDateTime ahora = LocalDateTime.now(LIMA);
        bloqueoRepository.borrarVencidos(ahora);       // limpieza de bloqueos vencidos
        bloqueoRepository.liberarDelCliente(cliente);  // un solo bloqueo por cliente

        boolean libre = turnoService
                .turnosLibres(req.getIdServicio(), req.getIdMecanico(), req.getFecha(), cliente.getIdUsuario())
                .stream()
                .anyMatch(t -> t.getHoraInicio().equals(req.getHoraInicio()));
        if (!libre) {
            throw new ConflictException("El turno no esta disponible");
        }

        LocalTime horaFin = req.getHoraInicio().plusMinutes(servicio.getDuracionMinutos());

        BloqueoTurno bloqueo = new BloqueoTurno();
        bloqueo.setCliente(cliente);
        bloqueo.setMecanico(mecanico);
        bloqueo.setFecha(req.getFecha());
        bloqueo.setHoraInicio(req.getHoraInicio());
        bloqueo.setHoraFin(horaFin);
        bloqueo.setExpiraEn(ahora.plusMinutes(MINUTOS_BLOQUEO));
        bloqueo = bloqueoRepository.save(bloqueo);

        BloqueoResponse res = new BloqueoResponse();
        res.setIdBloqueo(bloqueo.getIdBloqueo());
        res.setFecha(bloqueo.getFecha().toString());
        res.setHoraInicio(bloqueo.getHoraInicio().toString());
        res.setHoraFin(bloqueo.getHoraFin().toString());
        res.setExpiraEn(bloqueo.getExpiraEn().toString());
        res.setSegundosRestantes(Duration.between(ahora, bloqueo.getExpiraEn()).getSeconds());
        return res;
    }

    @Transactional
    public void liberar(String correoCliente, Long idBloqueo) {
        BloqueoTurno bloqueo = bloqueoRepository.findById(idBloqueo)
                .orElseThrow(() -> new NotFoundException("Bloqueo no encontrado"));
        if (!bloqueo.getCliente().getCorreo().equalsIgnoreCase(correoCliente)) {
            throw new ForbiddenException("Ese bloqueo no es tuyo");
        }
        bloqueoRepository.delete(bloqueo);
    }
}