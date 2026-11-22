package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.HistorialVehiculoResponse;
import com.tecsup.autosmart.service.HistorialVehiculoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class AdminHistorialVehiculoController {

    private final HistorialVehiculoService historialService;

    public AdminHistorialVehiculoController(HistorialVehiculoService historialService) {
        this.historialService = historialService;
    }

    @GetMapping("/admin/vehicles/{placa}/history")
    public ResponseEntity<HistorialVehiculoResponse> historial(@PathVariable String placa) {
        return ResponseEntity.ok(historialService.historialPorPlaca(placa));
    }
}