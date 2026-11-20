package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.PagoRequest;
import com.tecsup.autosmart.dto.PagoResponse;
import com.tecsup.autosmart.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/payments")
    public ResponseEntity<PagoResponse> pagar(@Valid @RequestBody PagoRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.pagar(auth.getName(), request));
    }
}