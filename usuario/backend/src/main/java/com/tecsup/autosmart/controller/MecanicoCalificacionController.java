package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.MisCalificacionesResponse;
import com.tecsup.autosmart.service.CalificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class MecanicoCalificacionController {

    private final CalificacionService calificacionService;

    public MecanicoCalificacionController(CalificacionService calificacionService) {
        this.calificacionService = calificacionService;
    }

    @GetMapping("/mechanics/me/ratings")
    public ResponseEntity<MisCalificacionesResponse> misCalificaciones(Authentication auth) {
        return ResponseEntity.ok(calificacionService.misCalificaciones(auth.getName()));
    }
}