package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.VehiculoRequest;
import com.tecsup.autosmart.dto.VehiculoResponse;
import com.tecsup.autosmart.service.VehiculoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> registrar(@RequestBody VehiculoRequest request) {
        try {
            VehiculoResponse response = vehiculoService.registrarVehiculo(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<VehiculoResponse>> listar() {
        return ResponseEntity.ok(vehiculoService.listarTodos());
    }
}