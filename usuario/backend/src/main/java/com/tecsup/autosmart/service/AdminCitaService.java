package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CitaAdminResponse;
import com.tecsup.autosmart.dto.PaginaResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.repository.CitaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class AdminCitaService {

    private final CitaRepository citaRepository;

    public AdminCitaService(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
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