package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.IngresoStockRequest;
import com.tecsup.autosmart.dto.MovimientoResponse;
import com.tecsup.autosmart.service.KardexService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/materials/{id}")
@CrossOrigin(origins = "*")
public class AdminKardexController {

    private final KardexService kardexService;

    public AdminKardexController(KardexService kardexService) {
        this.kardexService = kardexService;
    }

    @PostMapping("/entries")
    public ResponseEntity<MovimientoResponse> registrarEntrada(@PathVariable Long id,
                                                               @Valid @RequestBody IngresoStockRequest request,
                                                               Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kardexService.registrarEntrada(auth.getName(), id, request));
    }

    @GetMapping("/kardex")
    public ResponseEntity<List<MovimientoResponse>> kardex(@PathVariable Long id) {
        return ResponseEntity.ok(kardexService.kardex(id));
    }
}