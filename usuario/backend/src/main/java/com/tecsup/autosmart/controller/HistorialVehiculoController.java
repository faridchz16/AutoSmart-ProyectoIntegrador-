package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.HistorialVehiculoResponse;
import com.tecsup.autosmart.service.HistorialVehiculoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class HistorialVehiculoController {

    private final HistorialVehiculoService historialService;

    public HistorialVehiculoController(HistorialVehiculoService historialService) {
        this.historialService = historialService;
    }

    @GetMapping("/vehicles/{id}/history")
    public ResponseEntity<HistorialVehiculoResponse> historial(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(historialService.historial(auth.getName(), id));
    }
}