package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.EstadoRequest;
import com.tecsup.autosmart.dto.ServicioRequest;
import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/services")
@CrossOrigin(origins = "*")
public class AdminServicioController {

    private final ServicioService servicioService;

    public AdminServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.ok(servicioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ServicioResponse> cambiarEstado(@PathVariable Long id,
                                                          @Valid @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(servicioService.cambiarEstado(id, request.getActivo()));
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponse>> listar() {
        return ResponseEntity.ok(servicioService.listarTodos());
    }
}