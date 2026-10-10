package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.ServicioRequest;
import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/services")
@CrossOrigin(origins = "*")
public class AdminServicioController {

    private final ServicioService servicioService;

    public AdminServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(request));
    }
}