package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.CalificacionAdminResponse;
import com.tecsup.autosmart.dto.OcultarCalificacionRequest;
import com.tecsup.autosmart.service.ModeracionCalificacionService;
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
@RequestMapping("/admin/ratings")
public class AdminCalificacionController {

    private final ModeracionCalificacionService moderacionService;

    public AdminCalificacionController(ModeracionCalificacionService moderacionService) {
        this.moderacionService = moderacionService;
    }

    @GetMapping
    public ResponseEntity<List<CalificacionAdminResponse>> listar(@RequestParam(required = false) Boolean oculto) {
        return ResponseEntity.ok(moderacionService.listar(oculto));
    }

    @PutMapping("/{id}/hide")
    public ResponseEntity<CalificacionAdminResponse> ocultar(@PathVariable Long id,
                                                             @Valid @RequestBody OcultarCalificacionRequest request,
                                                             Authentication auth) {
        return ResponseEntity.ok(moderacionService.ocultar(auth.getName(), id, request));
    }
}