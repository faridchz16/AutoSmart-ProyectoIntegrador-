package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.ServicioRequest;
import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.model.Servicio;
import com.tecsup.autosmart.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;

    public ServicioService(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    @Transactional
    public ServicioResponse crear(ServicioRequest request) {
        String nombre = request.getNombre().trim();

        if (servicioRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe un servicio con ese nombre");
        }

        Servicio servicio = new Servicio();
        servicio.setNombre(nombre);
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecioBase(request.getPrecioBase());
        servicio.setDuracionMinutos(request.getDuracionMinutos());
        servicio.setActivo(true);

        return mapearADTO(servicioRepository.save(servicio));
    }

    private ServicioResponse mapearADTO(Servicio s) {
        ServicioResponse res = new ServicioResponse();
        res.setIdServicio(s.getIdServicio());
        res.setNombre(s.getNombre());
        res.setDescripcion(s.getDescripcion());
        res.setPrecioBase(s.getPrecioBase());
        res.setDuracionMinutos(s.getDuracionMinutos());
        res.setActivo(s.getActivo());
        return res;
    }
}