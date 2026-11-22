package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.AtenderDerivacionRequest;
import com.tecsup.autosmart.dto.DerivacionResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.mapper.DerivacionMapper;
import com.tecsup.autosmart.model.Derivacion;
import com.tecsup.autosmart.model.EstadoDerivacion;
import com.tecsup.autosmart.model.MensajeChatbot;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.DerivacionRepository;
import com.tecsup.autosmart.repository.MensajeChatbotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DerivacionService {

    private static final int MAX_RESUMEN = 2000;

    private final DerivacionRepository derivacionRepository;
    private final MensajeChatbotRepository mensajeRepository;
    private final DerivacionMapper mapper;
    private final Clock clock;

    public DerivacionService(DerivacionRepository derivacionRepository,
                             MensajeChatbotRepository mensajeRepository,
                             DerivacionMapper mapper,
                             Clock clock) {
        this.derivacionRepository = derivacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<Derivacion> pendienteDe(String idConversacion) {
        return derivacionRepository.findFirstByIdConversacionAndEstado(idConversacion, EstadoDerivacion.PENDIENTE);
    }

    @Transactional(readOnly = true)
    public List<MensajeChatbot> ultimosMensajes(String idConversacion) {
        List<MensajeChatbot> mensajes = new ArrayList<>(
                mensajeRepository.findTop10ByIdConversacionOrderByIdMensajeDesc(idConversacion));
        Collections.reverse(mensajes);
        return mensajes;
    }

    @Transactional
    public Derivacion registrar(String idConversacion, Usuario usuario, String contacto, String mensajeActual) {
        String resumen = ultimosMensajes(idConversacion).stream()
                .map(m -> m.getEmisor().name() + ": " + m.getTexto())
                .collect(Collectors.joining("\n"));
        resumen = (resumen.isEmpty() ? "" : resumen + "\n") + "USUARIO: " + mensajeActual;
        if (resumen.length() > MAX_RESUMEN) {
            resumen = resumen.substring(resumen.length() - MAX_RESUMEN);
        }

        Derivacion derivacion = new Derivacion();
        derivacion.setIdConversacion(idConversacion);
        derivacion.setUsuario(usuario);
        derivacion.setContacto(contacto);
        derivacion.setResumen(resumen);
        derivacion.setEstado(EstadoDerivacion.PENDIENTE);
        derivacion.setFecha(ahora());
        return derivacionRepository.save(derivacion);
    }

    @Transactional(readOnly = true)
    public List<DerivacionResponse> listar(String estado) {
        return derivacionRepository.buscar(estadoDesde(estado)).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public DerivacionResponse atender(String correoAdmin, Long idDerivacion, AtenderDerivacionRequest request) {
        Derivacion derivacion = derivacionRepository.findById(idDerivacion)
                .orElseThrow(() -> new NotFoundException("Derivación no encontrada"));
        if (derivacion.getEstado() == EstadoDerivacion.ATENDIDA) {
            throw new ConflictException("La derivación ya fue atendida");
        }
        derivacion.setEstado(EstadoDerivacion.ATENDIDA);
        derivacion.setNota(request.nota().trim());
        derivacion.setAtendidoPor(correoAdmin);
        derivacion.setFechaAtencion(ahora());
        return mapper.toResponse(derivacionRepository.save(derivacion));
    }

    private EstadoDerivacion estadoDesde(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoDerivacion.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Estado invalido. Usa PENDIENTE o ATENDIDA");
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).withNano(0);
    }
}