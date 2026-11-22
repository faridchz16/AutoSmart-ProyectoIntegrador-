package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.CalificacionRequest;
import com.tecsup.autosmart.dto.CalificacionResponse;
import com.tecsup.autosmart.service.CalificacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class CalificacionController {

    private final CalificacionService calificacionService;

    public CalificacionController(CalificacionService calificacionService) {
        this.calificacionService = calificacionService;
    }

    @PostMapping("/work-orders/{id}/rating")
    public ResponseEntity<CalificacionResponse> calificar(@PathVariable Long id,
                                                          @Valid @RequestBody CalificacionRequest request,
                                                          Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calificacionService.calificar(auth.getName(), id, request));
    }
}