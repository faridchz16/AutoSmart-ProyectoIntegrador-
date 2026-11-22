package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.ListaPagosResponse;
import com.tecsup.autosmart.dto.PagoEfectivoRequest;
import com.tecsup.autosmart.dto.PagoResponse;
import com.tecsup.autosmart.service.AdminPagoService;
import com.tecsup.autosmart.service.PagoEfectivoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/admin/payments")
public class AdminPagoController {

    private final PagoEfectivoService pagoEfectivoService;
    private final AdminPagoService adminPagoService;

    public AdminPagoController(PagoEfectivoService pagoEfectivoService, AdminPagoService adminPagoService) {
        this.pagoEfectivoService = pagoEfectivoService;
        this.adminPagoService = adminPagoService;
    }

    @GetMapping
    public ResponseEntity<ListaPagosResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String metodo,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(adminPagoService.listar(desde, hasta, metodo, estado));
    }

    @PostMapping("/cash")
    public ResponseEntity<PagoResponse> registrarEfectivo(@Valid @RequestBody PagoEfectivoRequest request,
                                                          Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoEfectivoService.registrar(auth.getName(), request));
    }
}