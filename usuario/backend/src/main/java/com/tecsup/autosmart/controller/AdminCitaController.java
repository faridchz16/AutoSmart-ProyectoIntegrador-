package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.CitaAdminResponse;
import com.tecsup.autosmart.dto.MecanicoLibreResponse;
import com.tecsup.autosmart.dto.PaginaResponse;
import com.tecsup.autosmart.dto.ReasignarRequest;
import com.tecsup.autosmart.service.AdminCitaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/appointments")
@CrossOrigin(origins = "*")
public class AdminCitaController {

    private final AdminCitaService adminCitaService;

    public AdminCitaController(AdminCitaService adminCitaService) {
        this.adminCitaService = adminCitaService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<CitaAdminResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer idMecanico,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminCitaService.listar(desde, hasta, estado, idMecanico, q, page, size));
    }

    @GetMapping("/{id}/available-mechanics")
    public ResponseEntity<List<MecanicoLibreResponse>> mecanicosLibres(@PathVariable Long id) {
        return ResponseEntity.ok(adminCitaService.mecanicosLibres(id));
    }

    @PutMapping("/{id}/reassign")
    public ResponseEntity<CitaAdminResponse> reasignar(@PathVariable Long id,
                                                       @Valid @RequestBody ReasignarRequest request) {
        return ResponseEntity.ok(adminCitaService.reasignar(id, request.getIdMecanico()));
    }
}