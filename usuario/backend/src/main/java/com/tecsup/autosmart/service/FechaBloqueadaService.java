package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.FechaBloqueadaRequest;
import com.tecsup.autosmart.dto.FechaBloqueadaResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.FechaBloqueada;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.FechaBloqueadaRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FechaBloqueadaService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final UsuarioRepository usuarioRepository;
    private final FechaBloqueadaRepository bloqueoRepository;
    private final CitaRepository citaRepository;

    public FechaBloqueadaService(UsuarioRepository usuarioRepository,
                                 FechaBloqueadaRepository bloqueoRepository,
                                 CitaRepository citaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.bloqueoRepository = bloqueoRepository;
        this.citaRepository = citaRepository;
    }

    // Por defecto lista las fechas bloqueadas desde hoy en adelante
    @Transactional(readOnly = true)
    public List<FechaBloqueadaResponse> listar(String correo, LocalDate desde) {
        Usuario mecanico = buscarMecanico(correo);
        LocalDate inicio = desde != null ? desde : LocalDate.now(ZONA_LIMA);
        return bloqueoRepository.findByMecanicoAndFechaGreaterThanEqualOrderByFechaAsc(mecanico, inicio)
                .stream().map(this::mapear).collect(Collectors.toList());
    }

    @Transactional
    public FechaBloqueadaResponse bloquear(String correo, FechaBloqueadaRequest request) {
        Usuario mecanico = buscarMecanico(correo);
        LocalDate fecha = request.getFecha();

        if (fecha.isBefore(LocalDate.now(ZONA_LIMA))) {
            throw new BadRequestException("No puedes bloquear una fecha pasada");
        }
        if (bloqueoRepository.existsByMecanicoAndFecha(mecanico, fecha)) {
            throw new ConflictException("Esa fecha ya esta bloqueada");
        }
        if (citaRepository.existsByMecanicoAndFechaAndEstado(mecanico, fecha, EstadoCita.CONFIRMADA)) {
            throw new ConflictException("No puedes bloquear una fecha con citas confirmadas");
        }

        FechaBloqueada bloqueo = new FechaBloqueada();
        bloqueo.setMecanico(mecanico);
        bloqueo.setFecha(fecha);
        String motivo = request.getMotivo();
        bloqueo.setMotivo(motivo == null || motivo.isBlank() ? null : motivo.trim());
        return mapear(bloqueoRepository.save(bloqueo));
    }

    @Transactional
    public void desbloquear(String correo, Integer idBloqueo) {
        Usuario mecanico = buscarMecanico(correo);
        FechaBloqueada bloqueo = bloqueoRepository.findByIdBloqueoAndMecanico(idBloqueo, mecanico)
                .orElseThrow(() -> new NotFoundException("Fecha bloqueada no encontrada"));
        bloqueoRepository.delete(bloqueo);
    }

    private Usuario buscarMecanico(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private FechaBloqueadaResponse mapear(FechaBloqueada b) {
        FechaBloqueadaResponse res = new FechaBloqueadaResponse();
        res.setIdBloqueo(b.getIdBloqueo());
        res.setFecha(b.getFecha().toString());
        res.setMotivo(b.getMotivo());
        return res;
    }
}