package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.MaterialResponse;
import com.tecsup.autosmart.dto.SolicitudMaterialRequest;
import com.tecsup.autosmart.dto.SolicitudMaterialResponse;
import com.tecsup.autosmart.service.MaterialService;
import com.tecsup.autosmart.service.SolicitudMaterialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class SolicitudMaterialController {

    private final SolicitudMaterialService solicitudService;
    private final MaterialService materialService;

    public SolicitudMaterialController(SolicitudMaterialService solicitudService, MaterialService materialService) {
        this.solicitudService = solicitudService;
        this.materialService = materialService;
    }

    @GetMapping("/materials")
    public ResponseEntity<List<MaterialResponse>> buscarMateriales(@RequestParam(required = false) String buscar) {
        return ResponseEntity.ok(materialService.listar(buscar, false));
    }

    @PostMapping("/work-orders/{id}/material-requests")
    public ResponseEntity<SolicitudMaterialResponse> solicitar(@PathVariable Long id,
                                                               @Valid @RequestBody SolicitudMaterialRequest request,
                                                               Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitudService.solicitar(auth.getName(), id, request));
    }

    @GetMapping("/work-orders/{id}/material-requests")
    public ResponseEntity<List<SolicitudMaterialResponse>> listar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(solicitudService.listar(auth.getName(), id));
    }
}