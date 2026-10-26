package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.BloqueoRequest;
import com.tecsup.autosmart.dto.BloqueoResponse;
import com.tecsup.autosmart.service.BloqueoTurnoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/appointments/holds")
@CrossOrigin(origins = "*")
public class BloqueoTurnoController {

    private final BloqueoTurnoService bloqueoService;

    public BloqueoTurnoController(BloqueoTurnoService bloqueoService) {
        this.bloqueoService = bloqueoService;
    }

    @PostMapping
    public ResponseEntity<BloqueoResponse> bloquear(@Valid @RequestBody BloqueoRequest request,
                                                    Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bloqueoService.bloquear(auth.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> liberar(@PathVariable Long id, Authentication auth) {
        bloqueoService.liberar(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}