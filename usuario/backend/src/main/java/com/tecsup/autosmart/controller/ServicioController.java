package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.MecanicoDisponibleResponse;
import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.dto.TurnoResponse;
import com.tecsup.autosmart.service.DisponibilidadService;
import com.tecsup.autosmart.service.ServicioService;
import com.tecsup.autosmart.service.TurnoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/services")
@CrossOrigin(origins = "*")
public class ServicioController {

    private final ServicioService servicioService;
    private final DisponibilidadService disponibilidadService;
    private final TurnoService turnoService;

    public ServicioController(ServicioService servicioService,
                              DisponibilidadService disponibilidadService,
                              TurnoService turnoService) {
        this.servicioService = servicioService;
        this.disponibilidadService = disponibilidadService;
        this.turnoService = turnoService;
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

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<TurnoResponse>> turnos(
            @PathVariable Long id,
            @RequestParam("mecanico") Integer idMecanico,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoService.turnosLibres(id, idMecanico, fecha));
    }
}