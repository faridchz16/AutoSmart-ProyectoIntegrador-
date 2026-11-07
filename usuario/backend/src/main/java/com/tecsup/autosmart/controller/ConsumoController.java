package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.ConsumoRequest;
import com.tecsup.autosmart.dto.ConsumosOrdenResponse;
import com.tecsup.autosmart.service.ConsumoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/work-orders/{id}/consumptions")
@CrossOrigin(origins = "*")
public class ConsumoController {

    private final ConsumoService consumoService;

    public ConsumoController(ConsumoService consumoService) {
        this.consumoService = consumoService;
    }

    @PostMapping
    public ResponseEntity<ConsumosOrdenResponse> registrar(@PathVariable Long id,
                                                           @Valid @RequestBody ConsumoRequest request,
                                                           Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consumoService.registrar(auth.getName(), id, request));
    }

    @GetMapping
    public ResponseEntity<ConsumosOrdenResponse> listar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(consumoService.listar(auth.getName(), id));
    }
}