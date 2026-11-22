package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.HistorialVehiculoResponse;
import com.tecsup.autosmart.dto.HistorialVehiculoResponse.Mantenimiento;
import com.tecsup.autosmart.dto.HistorialVehiculoResponse.Propietario;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.model.Vehiculo;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import com.tecsup.autosmart.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistorialVehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final OrdenTrabajoRepository ordenRepository;

    public HistorialVehiculoService(VehiculoRepository vehiculoRepository, OrdenTrabajoRepository ordenRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.ordenRepository = ordenRepository;
    }

    @Transactional(readOnly = true)
    public HistorialVehiculoResponse historial(String correoCliente, Long idVehiculo) {
        Vehiculo vehiculo = vehiculoRepository.findById(idVehiculo)
                .orElseThrow(() -> new NotFoundException("Vehiculo no encontrado"));
        if (vehiculo.getCliente() == null || !vehiculo.getCliente().getCorreo().equalsIgnoreCase(correoCliente)) {
            throw new ForbiddenException("El vehiculo no te pertenece");
        }
        return armar(vehiculo);
    }

    @Transactional(readOnly = true)
    public HistorialVehiculoResponse historialPorPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new BadRequestException("La placa es obligatoria");
        }
        Vehiculo vehiculo = vehiculoRepository.findByPlacaIgnoreCase(placa.trim())
                .orElseThrow(() -> new NotFoundException("No existe un vehiculo con la placa " + placa.trim().toUpperCase()));
        return armar(vehiculo);
    }

    private HistorialVehiculoResponse armar(Vehiculo vehiculo) {
        List<Mantenimiento> mantenimientos = ordenRepository.historialDelVehiculo(vehiculo.getIdVehiculo()).stream()
                .map(this::aMantenimiento)
                .collect(Collectors.toList());

        HistorialVehiculoResponse res = new HistorialVehiculoResponse();
        res.setIdVehiculo(vehiculo.getIdVehiculo());
        res.setPlaca(vehiculo.getPlaca());
        res.setMarca(vehiculo.getMarca());
        res.setModelo(vehiculo.getModelo());
        res.setAnio(vehiculo.getAnio());
        res.setKilometraje(vehiculo.getKilometraje());
        res.setPropietario(propietario(vehiculo.getCliente()));
        res.setTotalMantenimientos(mantenimientos.size());
        res.setMantenimientos(mantenimientos);
        return res;
    }

    private Propietario propietario(Usuario cliente) {
        return cliente == null ? null : new Propietario(cliente.getNombre(), cliente.getCorreo());
    }

    private Mantenimiento aMantenimiento(OrdenTrabajo o) {
        return new Mantenimiento(
                o.getIdOrden(),
                o.getCita().getIdCita(),
                o.getCita().getTicket(),
                o.getCita().getEstado().name(),
                o.getCita().getServicio().getNombre(),
                o.getCita().getMecanico().getNombre(),
                o.getFechaInicio().toString(),
                o.getFechaFin() == null ? null : o.getFechaFin().toString(),
                o.getDiagnostico(),
                o.getCostoRepuestos());
    }
}