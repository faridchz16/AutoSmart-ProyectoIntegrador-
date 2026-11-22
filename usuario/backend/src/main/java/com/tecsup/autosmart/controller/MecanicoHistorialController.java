package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.OrdenHistorialMecanicoResponse;
import com.tecsup.autosmart.service.HistorialMecanicoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class MecanicoHistorialController {

    private final HistorialMecanicoService historialService;

    public MecanicoHistorialController(HistorialMecanicoService historialService) {
        this.historialService = historialService;
    }

    @GetMapping("/mechanics/me/history")
    public ResponseEntity<List<OrdenHistorialMecanicoResponse>> historial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication auth) {
        return ResponseEntity.ok(historialService.historial(auth.getName(), desde, hasta));
    }
}