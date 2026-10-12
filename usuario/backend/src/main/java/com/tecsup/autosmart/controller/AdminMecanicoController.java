package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.MecanicoCreadoResponse;
import com.tecsup.autosmart.dto.MecanicoRequest;
import com.tecsup.autosmart.service.AdminUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/mechanics")
@CrossOrigin(origins = "*")
public class AdminMecanicoController {

    private final AdminUsuarioService adminUsuarioService;

    public AdminMecanicoController(AdminUsuarioService adminUsuarioService) {
        this.adminUsuarioService = adminUsuarioService;
    }

    @PostMapping
    public ResponseEntity<MecanicoCreadoResponse> crear(@Valid @RequestBody MecanicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUsuarioService.crearMecanico(request));
    }
}