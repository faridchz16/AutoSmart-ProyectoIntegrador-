package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.DiagnosticoRequest;
import com.tecsup.autosmart.dto.DiagnosticoResponse;
import com.tecsup.autosmart.service.DiagnosticoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/work-orders/{id}/diagnosis")
@CrossOrigin(origins = "*")
public class DiagnosticoController {

    private final DiagnosticoService diagnosticoService;

    public DiagnosticoController(DiagnosticoService diagnosticoService) {
        this.diagnosticoService = diagnosticoService;
    }

    @PutMapping
    public ResponseEntity<DiagnosticoResponse> registrar(@PathVariable Long id,
                                                         @Valid @RequestBody DiagnosticoRequest request,
                                                         Authentication auth) {
        return ResponseEntity.ok(diagnosticoService.registrar(auth.getName(), id, request));
    }

    @GetMapping
    public ResponseEntity<DiagnosticoResponse> obtener(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(diagnosticoService.obtener(auth.getName(), id));
    }
}