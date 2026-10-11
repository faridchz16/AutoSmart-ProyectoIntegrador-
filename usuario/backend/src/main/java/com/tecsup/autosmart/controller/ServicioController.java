package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.service.ServicioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@CrossOrigin(origins = "*")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponse>> listar(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(servicioService.listarActivos(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(servicioService.obtenerActivo(id));
    }
}