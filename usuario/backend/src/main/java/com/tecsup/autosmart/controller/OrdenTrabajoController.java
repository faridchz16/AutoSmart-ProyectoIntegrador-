package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.OrdenTrabajoResponse;
import com.tecsup.autosmart.service.OrdenTrabajoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class OrdenTrabajoController {

    private final OrdenTrabajoService ordenService;

    public OrdenTrabajoController(OrdenTrabajoService ordenService) {
        this.ordenService = ordenService;
    }

    @PutMapping("/appointments/{id}/start")
    public ResponseEntity<OrdenTrabajoResponse> iniciar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ordenService.iniciarAtencion(auth.getName(), id));
    }
    @GetMapping("/mechanics/me/work-orders")
    public ResponseEntity<List<OrdenTrabajoResponse>> misOrdenes(Authentication auth) {
        return ResponseEntity.ok(ordenService.ordenesDelMecanico(auth.getName()));
    }
}