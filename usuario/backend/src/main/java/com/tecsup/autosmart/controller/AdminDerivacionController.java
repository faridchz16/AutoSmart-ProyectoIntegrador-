package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.AtenderDerivacionRequest;
import com.tecsup.autosmart.dto.DerivacionResponse;
import com.tecsup.autosmart.service.DerivacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/admin/chatbot/handoffs")
public class AdminDerivacionController {

    private final DerivacionService derivacionService;

    public AdminDerivacionController(DerivacionService derivacionService) {
        this.derivacionService = derivacionService;
    }

    @GetMapping
    public ResponseEntity<List<DerivacionResponse>> listar(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(derivacionService.listar(estado));
    }

    @PutMapping("/{id}/attend")
    public ResponseEntity<DerivacionResponse> atender(@PathVariable Long id,
                                                      @Valid @RequestBody AtenderDerivacionRequest request,
                                                      Authentication auth) {
        return ResponseEntity.ok(derivacionService.atender(auth.getName(), id, request));
    }
}