package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.CitaDetalleResponse;
import com.tecsup.autosmart.service.CitaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/appointments")
@CrossOrigin(origins = "*")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CitaDetalleResponse> detalle(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(citaService.detalleParaMecanico(auth.getName(), id));
    }
}