package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.HorarioRequest;
import com.tecsup.autosmart.dto.HorarioResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.HorarioMecanico;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.HorarioMecanicoRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HorarioMecanicoService {

    private final UsuarioRepository usuarioRepository;
    private final HorarioMecanicoRepository horarioRepository;

    public HorarioMecanicoService(UsuarioRepository usuarioRepository,
                                  HorarioMecanicoRepository horarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.horarioRepository = horarioRepository;
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> listar(String correo) {
        Usuario mecanico = buscarMecanico(correo);
        return horarioRepository.findByMecanicoOrderByDiaSemanaAscHoraInicioAsc(mecanico)
                .stream().map(this::mapear).collect(Collectors.toList());
    }

    @Transactional
    public HorarioResponse crear(String correo, HorarioRequest request) {
        Usuario mecanico = buscarMecanico(correo);
        validarRango(mecanico, request, -1);

        HorarioMecanico horario = new HorarioMecanico();
        horario.setMecanico(mecanico);
        aplicar(horario, request);
        return mapear(horarioRepository.save(horario));
    }

    @Transactional
    public HorarioResponse actualizar(String correo, Integer idHorario, HorarioRequest request) {
        Usuario mecanico = buscarMecanico(correo);
        HorarioMecanico horario = buscarHorario(idHorario, mecanico);
        validarRango(mecanico, request, idHorario);

        aplicar(horario, request);
        return mapear(horarioRepository.save(horario));
    }

    @Transactional
    public void eliminar(String correo, Integer idHorario) {
        Usuario mecanico = buscarMecanico(correo);
        horarioRepository.delete(buscarHorario(idHorario, mecanico));
    }

    private void validarRango(Usuario mecanico, HorarioRequest request, Integer excluirId) {
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new BadRequestException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (horarioRepository.haySolapamiento(mecanico, request.getDiaSemana(),
                request.getHoraInicio(), request.getHoraFin(), excluirId)) {
            throw new ConflictException("El horario se cruza con otro rango registrado ese dia");
        }
    }

    private void aplicar(HorarioMecanico horario, HorarioRequest request) {
        horario.setDiaSemana(request.getDiaSemana());
        horario.setHoraInicio(request.getHoraInicio());
        horario.setHoraFin(request.getHoraFin());
    }

    private Usuario buscarMecanico(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    // Un horario de otro mecanico responde 404 (no se revela que existe)
    private HorarioMecanico buscarHorario(Integer idHorario, Usuario mecanico) {
        return horarioRepository.findByIdHorarioAndMecanico(idHorario, mecanico)
                .orElseThrow(() -> new NotFoundException("Horario no encontrado"));
    }

    private HorarioResponse mapear(HorarioMecanico h) {
        HorarioResponse res = new HorarioResponse();
        res.setIdHorario(h.getIdHorario());
        res.setDiaSemana(h.getDiaSemana());
        res.setHoraInicio(h.getHoraInicio().toString());
        res.setHoraFin(h.getHoraFin().toString());
        return res;
    }
}