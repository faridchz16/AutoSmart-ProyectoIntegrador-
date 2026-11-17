package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.MetodoPagoResponse;
import com.tecsup.autosmart.service.MetodoPagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class MetodoPagoController {

    private final MetodoPagoService metodoPagoService;

    public MetodoPagoController(MetodoPagoService metodoPagoService) {
        this.metodoPagoService = metodoPagoService;
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<List<MetodoPagoResponse>> listar() {
        return ResponseEntity.ok(metodoPagoService.listarHabilitados());
    }
}