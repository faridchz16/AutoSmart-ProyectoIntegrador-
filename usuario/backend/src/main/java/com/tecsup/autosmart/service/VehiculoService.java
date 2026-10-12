package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.VehiculoRequest;
import com.tecsup.autosmart.dto.VehiculoResponse;
import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.model.Vehiculo;
import com.tecsup.autosmart.repository.UsuarioRepository;
import com.tecsup.autosmart.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehiculoService {

    private static final String FORMATO_PLACA = "^[A-Z0-9]{3}-[0-9]{3}$";

    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository, UsuarioRepository usuarioRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public VehiculoResponse registrarVehiculo(VehiculoRequest request, String correoCliente) {
        String placa = request.getPlaca() == null ? "" : request.getPlaca().trim().toUpperCase();

        if (!placa.matches(FORMATO_PLACA)) {
            throw new BadRequestException("Formato de placa inválido. Usa el formato ABC-123");
        }
        if (vehiculoRepository.existsByPlaca(placa)) {
            throw new ConflictException("La placa ya se encuentra registrada en el sistema");
        }

        Usuario cliente = usuarioRepository.findByCorreo(correoCliente)
                .orElseThrow(() -> new NotFoundException("Usuario cliente no encontrado"));

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca(placa);
        vehiculo.setMarca(request.getMarca());
        vehiculo.setModelo(request.getModelo());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setKilometraje(request.getKilometraje() != null ? request.getKilometraje() : 0);
        vehiculo.setCliente(cliente);

        return mapearADTO(vehiculoRepository.save(vehiculo));
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarPorCorreo(String correo) {
        Usuario cliente = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new NotFoundException("Usuario cliente no encontrado"));

        return vehiculoRepository.findByCliente(cliente).stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehiculoResponse actualizarVehiculo(Long id, VehiculoRequest request, String correoCliente) {
        Vehiculo vehiculo = buscarDelCliente(id, correoCliente);

        String placa = request.getPlaca() == null ? "" : request.getPlaca().trim().toUpperCase();
        if (!placa.matches(FORMATO_PLACA)) {
            throw new BadRequestException("Formato de placa inválido. Usa el formato ABC-123");
        }
        if (vehiculoRepository.existsByPlacaAndIdVehiculoNot(placa, id)) {
            throw new ConflictException("La placa ya se encuentra registrada en el sistema");
        }

        if (request.getMarca() == null || request.getMarca().isBlank()) {
            throw new BadRequestException("La marca es obligatoria");
        }
        if (request.getModelo() == null || request.getModelo().isBlank()) {
            throw new BadRequestException("El modelo es obligatorio");
        }
        if (request.getAnio() == null) {
            throw new BadRequestException("El año es obligatorio");
        }

        // El kilometraje solo puede mantenerse o aumentar
        int actual = vehiculo.getKilometraje() != null ? vehiculo.getKilometraje() : 0;
        int nuevo = request.getKilometraje() != null ? request.getKilometraje() : actual;
        if (nuevo < 0) {
            throw new BadRequestException("El kilometraje no puede ser negativo");
        }
        if (nuevo < actual) {
            throw new BadRequestException("El kilometraje no puede ser menor al registrado (" + actual + " km)");
        }

        vehiculo.setPlaca(placa);
        vehiculo.setMarca(request.getMarca().trim());
        vehiculo.setModelo(request.getModelo().trim());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setKilometraje(nuevo);

        return mapearADTO(vehiculoRepository.save(vehiculo));
    }

    // Si el vehiculo no existe o es de otro cliente, responde igual (404) para no revelar su existencia
    private Vehiculo buscarDelCliente(Long id, String correoCliente) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehiculo no encontrado"));

        if (vehiculo.getCliente() == null
                || !vehiculo.getCliente().getCorreo().equalsIgnoreCase(correoCliente)) {
            throw new NotFoundException("Vehiculo no encontrado");
        }
        return vehiculo;
    }

    private VehiculoResponse mapearADTO(Vehiculo v) {
        VehiculoResponse res = new VehiculoResponse();
        res.setIdVehiculo(v.getIdVehiculo());
        res.setPlaca(v.getPlaca());
        res.setMarca(v.getMarca());
        res.setModelo(v.getModelo());
        res.setAnio(v.getAnio());
        res.setKilometraje(v.getKilometraje());
        if (v.getCliente() != null) {
            res.setIdCliente(v.getCliente().getIdUsuario());
            res.setNombreCliente(v.getCliente().getNombre());
        } else {
            res.setNombreCliente("Sin asignar");
        }
        return res;
    }
}