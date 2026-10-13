package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.PerfilMecanicoRequest;
import com.tecsup.autosmart.dto.PerfilMecanicoResponse;
import com.tecsup.autosmart.service.MecanicoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mechanics")
@CrossOrigin(origins = "*")
public class MecanicoController {

    private final MecanicoService mecanicoService;

    public MecanicoController(MecanicoService mecanicoService) {
        this.mecanicoService = mecanicoService;
    }

    @GetMapping("/me")
    public ResponseEntity<PerfilMecanicoResponse> obtener(Authentication auth) {
        return ResponseEntity.ok(mecanicoService.obtenerPerfil(auth.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<PerfilMecanicoResponse> guardar(@Valid @RequestBody PerfilMecanicoRequest request,
                                                          Authentication auth) {
        return ResponseEntity.ok(mecanicoService.guardarPerfil(auth.getName(), request));
    }
}