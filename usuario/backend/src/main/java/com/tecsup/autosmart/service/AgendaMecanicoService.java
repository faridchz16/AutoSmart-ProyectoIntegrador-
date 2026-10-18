package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CitaAgendaResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgendaMecanicoService {

    // El taller opera en hora de Lima (la conexion JDBC usa UTC)
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final long MAX_DIAS_RANGO = 31;

    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;

    public AgendaMecanicoService(UsuarioRepository usuarioRepository, CitaRepository citaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
    }

    // Sin parametros devuelve las citas de hoy; con desde/hasta, la agenda de ese rango
    @Transactional(readOnly = true)
    public List<CitaAgendaResponse> agenda(String correo, LocalDate desde, LocalDate hasta) {
        Usuario mecanico = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        LocalDate inicio = desde != null ? desde : LocalDate.now(ZONA_LIMA);
        LocalDate fin = hasta != null ? hasta : inicio;

        if (fin.isBefore(inicio)) {
            throw new BadRequestException("La fecha 'hasta' no puede ser anterior a 'desde'");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > MAX_DIAS_RANGO) {
            throw new BadRequestException("El rango no puede superar " + MAX_DIAS_RANGO + " dias");
        }

        return citaRepository.buscarAgenda(mecanico, inicio, fin).stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    private CitaAgendaResponse mapear(Cita c) {
        CitaAgendaResponse res = new CitaAgendaResponse();
        res.setIdCita(c.getIdCita());
        res.setTicket(c.getTicket());
        res.setFecha(c.getFecha().toString());
        res.setHoraInicio(c.getHoraInicio().toString());
        res.setHoraFin(c.getHoraFin().toString());
        res.setEstado(c.getEstado().name());
        res.setServicio(c.getServicio().getNombre());
        res.setCliente(c.getCliente().getNombre());
        res.setPlaca(c.getVehiculo().getPlaca());
        return res;
    }
}