package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.DetalleMantenimientoResponse;
import com.tecsup.autosmart.service.DetalleMantenimientoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class DetalleMantenimientoController {

    private final DetalleMantenimientoService detalleService;

    public DetalleMantenimientoController(DetalleMantenimientoService detalleService) {
        this.detalleService = detalleService;
    }

    @GetMapping("/work-orders/{id}/detail")
    public ResponseEntity<DetalleMantenimientoResponse> detalle(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(detalleService.detalle(auth.getName(), id));
    }
}