package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.FechaBloqueadaRequest;
import com.tecsup.autosmart.dto.FechaBloqueadaResponse;
import com.tecsup.autosmart.service.FechaBloqueadaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/mechanics/me/blocked-dates")
@CrossOrigin(origins = "*")
public class FechaBloqueadaController {

    private final FechaBloqueadaService bloqueoService;

    public FechaBloqueadaController(FechaBloqueadaService bloqueoService) {
        this.bloqueoService = bloqueoService;
    }

    @GetMapping
    public ResponseEntity<List<FechaBloqueadaResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            Authentication auth) {
        return ResponseEntity.ok(bloqueoService.listar(auth.getName(), desde));
    }

    @PostMapping
    public ResponseEntity<FechaBloqueadaResponse> bloquear(@Valid @RequestBody FechaBloqueadaRequest request,
                                                           Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bloqueoService.bloquear(auth.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desbloquear(@PathVariable Integer id, Authentication auth) {
        bloqueoService.desbloquear(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}