package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.PagoEfectivoRequest;
import com.tecsup.autosmart.dto.PagoResponse;
import com.tecsup.autosmart.service.PagoEfectivoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/admin/payments")
public class AdminPagoController {

    private final PagoEfectivoService pagoEfectivoService;

    public AdminPagoController(PagoEfectivoService pagoEfectivoService) {
        this.pagoEfectivoService = pagoEfectivoService;
    }

    @PostMapping("/cash")
    public ResponseEntity<PagoResponse> registrarEfectivo(@Valid @RequestBody PagoEfectivoRequest request,
                                                          Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoEfectivoService.registrar(auth.getName(), request));
    }
}