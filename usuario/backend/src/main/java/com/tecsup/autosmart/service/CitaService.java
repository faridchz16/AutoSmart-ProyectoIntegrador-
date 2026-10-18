package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CitaDetalleResponse;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.repository.CitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitaService {

    private final CitaRepository citaRepository;

    public CitaService(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    // Solo el mecanico asignado a la cita puede ver su detalle
    @Transactional(readOnly = true)
    public CitaDetalleResponse detalleParaMecanico(String correoMecanico, Long idCita) {
        Cita cita = citaRepository.buscarDetalle(idCita)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));

        if (!cita.getMecanico().getCorreo().equalsIgnoreCase(correoMecanico)) {
            throw new ForbiddenException("Solo el mecanico asignado puede ver esta cita");
        }
        return mapear(cita);
    }

    private CitaDetalleResponse mapear(Cita c) {
        CitaDetalleResponse res = new CitaDetalleResponse();
        res.setIdCita(c.getIdCita());
        res.setTicket(c.getTicket());
        res.setFecha(c.getFecha().toString());
        res.setHoraInicio(c.getHoraInicio().toString());
        res.setHoraFin(c.getHoraFin().toString());
        res.setEstado(c.getEstado().name());
        res.setPrecio(c.getPrecio());
        res.setServicio(c.getServicio().getNombre());
        res.setDuracionMinutos(c.getServicio().getDuracionMinutos());
        res.setCliente(c.getCliente().getNombre());
        res.setTelefonoCliente(c.getCliente().getTelefono());
        res.setPlaca(c.getVehiculo().getPlaca());
        res.setMarca(c.getVehiculo().getMarca());
        res.setModelo(c.getVehiculo().getModelo());
        res.setAnio(c.getVehiculo().getAnio());
        res.setKilometraje(c.getVehiculo().getKilometraje());
        return res;
    }
}