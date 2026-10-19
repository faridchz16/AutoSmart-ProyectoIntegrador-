package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.MecanicoDisponibleResponse;
import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.service.DisponibilidadService;
import com.tecsup.autosmart.service.ServicioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@CrossOrigin(origins = "*")
public class ServicioController {

    private final ServicioService servicioService;
    private final DisponibilidadService disponibilidadService;

    public ServicioController(ServicioService servicioService,
                              DisponibilidadService disponibilidadService) {
        this.servicioService = servicioService;
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponse>> listar(@RequestParam(required = false) String q,
                                                         @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(servicioService.listarActivos(q, categoria));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(servicioService.obtenerActivo(id));
    }

    @GetMapping("/{id}/mechanics")
    public ResponseEntity<List<MecanicoDisponibleResponse>> mecanicos(@PathVariable Long id) {
        return ResponseEntity.ok(disponibilidadService.mecanicosPorServicio(id));
    }
}