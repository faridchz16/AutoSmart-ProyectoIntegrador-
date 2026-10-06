package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.VehiculoRequest;
import com.tecsup.autosmart.dto.VehiculoResponse;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.model.Vehiculo;
import com.tecsup.autosmart.repository.UsuarioRepository;
import com.tecsup.autosmart.repository.VehiculoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository, UsuarioRepository usuarioRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public VehiculoResponse registrarVehiculo(VehiculoRequest request) {
        if (vehiculoRepository.existsByPlaca(request.getPlaca().trim().toUpperCase())) {
            throw new RuntimeException("La placa ya se encuentra registrada en el sistema");
        }

        Usuario cliente = null;
        if (request.getIdCliente() != null) {
            cliente = usuarioRepository.findById(request.getIdCliente())
                    .orElseThrow(() -> new RuntimeException("Usuario cliente no encontrado"));
        }

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca(request.getPlaca().trim().toUpperCase());
        vehiculo.setMarca(request.getMarca());
        vehiculo.setModelo(request.getModelo());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setKilometraje(request.getKilometraje() != null ? request.getKilometraje() : 0);
        vehiculo.setCliente(cliente);

        Vehiculo guardado = vehiculoRepository.save(vehiculo);
        return mapearADTO(guardado);
    }

    public List<VehiculoResponse> listarTodos() {
        return vehiculoRepository.findAll().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
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