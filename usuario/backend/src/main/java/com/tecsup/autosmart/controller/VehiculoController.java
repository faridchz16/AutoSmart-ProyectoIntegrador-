package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.VehiculoRequest;
import com.tecsup.autosmart.dto.VehiculoResponse;
import com.tecsup.autosmart.service.VehiculoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@CrossOrigin(origins = "*")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @PostMapping
    public ResponseEntity<VehiculoResponse> registrar(@RequestBody VehiculoRequest request, Authentication auth) {
        VehiculoResponse response = vehiculoService.registrarVehiculo(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<VehiculoResponse>> listarMisVehiculos(Authentication auth) {
        return ResponseEntity.ok(vehiculoService.listarPorCorreo(auth.getName()));
    }
}