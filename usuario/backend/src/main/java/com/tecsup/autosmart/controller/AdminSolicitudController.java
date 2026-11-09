package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.DecisionSolicitudRequest;
import com.tecsup.autosmart.dto.SolicitudMaterialResponse;
import com.tecsup.autosmart.model.EstadoSolicitud;
import com.tecsup.autosmart.service.AtencionSolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/material-requests")
@CrossOrigin(origins = "*")
public class AdminSolicitudController {

    private final AtencionSolicitudService atencionService;

    public AdminSolicitudController(AtencionSolicitudService atencionService) {
        this.atencionService = atencionService;
    }

    @GetMapping
    public ResponseEntity<List<SolicitudMaterialResponse>> listar(@RequestParam(required = false) EstadoSolicitud estado) {
        return ResponseEntity.ok(atencionService.listar(estado));
    }

    @PutMapping("/{id}/decision")
    public ResponseEntity<SolicitudMaterialResponse> decidir(@PathVariable Long id,
                                                             @Valid @RequestBody DecisionSolicitudRequest request,
                                                             Authentication auth) {
        return ResponseEntity.ok(atencionService.decidir(auth.getName(), id, request));
    }
}