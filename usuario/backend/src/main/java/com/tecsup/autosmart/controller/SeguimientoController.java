package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.SeguimientoResponse;
import com.tecsup.autosmart.service.SeguimientoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    public SeguimientoController(SeguimientoService seguimientoService) {
        this.seguimientoService = seguimientoService;
    }

    @GetMapping("/appointments/{id}/tracking")
    public ResponseEntity<SeguimientoResponse> seguimiento(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(seguimientoService.seguimiento(auth.getName(), id));
    }
}