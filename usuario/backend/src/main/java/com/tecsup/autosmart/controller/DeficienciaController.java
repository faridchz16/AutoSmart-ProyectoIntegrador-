package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.DeficienciaRequest;
import com.tecsup.autosmart.dto.DeficienciaResponse;
import com.tecsup.autosmart.service.DeficienciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/work-orders/{id}/deficiencies")
@CrossOrigin(origins = "*")
public class DeficienciaController {

    private final DeficienciaService deficienciaService;

    public DeficienciaController(DeficienciaService deficienciaService) {
        this.deficienciaService = deficienciaService;
    }

    @PostMapping
    public ResponseEntity<DeficienciaResponse> registrar(@PathVariable Long id,
                                                         @Valid @RequestBody DeficienciaRequest request,
                                                         Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deficienciaService.registrar(auth.getName(), id, request));
    }

    @GetMapping
    public ResponseEntity<List<DeficienciaResponse>> listar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(deficienciaService.listar(auth.getName(), id));
    }
}