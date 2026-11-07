package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.DecisionPresupuestoRequest;
import com.tecsup.autosmart.dto.PresupuestoRequest;
import com.tecsup.autosmart.dto.PresupuestoResponse;
import com.tecsup.autosmart.service.PresupuestoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class PresupuestoController {

    private final PresupuestoService presupuestoService;

    public PresupuestoController(PresupuestoService presupuestoService) {
        this.presupuestoService = presupuestoService;
    }

    @PostMapping("/work-orders/{id}/quote")
    public ResponseEntity<PresupuestoResponse> crear(@PathVariable Long id,
                                                     @Valid @RequestBody PresupuestoRequest request,
                                                     Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(presupuestoService.crear(auth.getName(), id, request));
    }

    @GetMapping("/work-orders/{id}/quote")
    public ResponseEntity<PresupuestoResponse> verComoMecanico(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(presupuestoService.obtenerParaMecanico(auth.getName(), id));
    }

    @GetMapping("/appointments/{idCita}/quote")
    public ResponseEntity<PresupuestoResponse> verComoCliente(@PathVariable Long idCita, Authentication auth) {
        return ResponseEntity.ok(presupuestoService.obtenerParaCliente(auth.getName(), idCita));
    }

    @PutMapping("/quotes/{idPresupuesto}/decision")
    public ResponseEntity<PresupuestoResponse> decidir(@PathVariable Long idPresupuesto,
                                                       @Valid @RequestBody DecisionPresupuestoRequest request,
                                                       Authentication auth) {
        return ResponseEntity.ok(presupuestoService.decidir(auth.getName(), idPresupuesto, request));
    }
}