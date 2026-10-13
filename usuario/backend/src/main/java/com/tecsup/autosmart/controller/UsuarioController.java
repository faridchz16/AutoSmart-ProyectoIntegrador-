package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.PerfilRequest;
import com.tecsup.autosmart.dto.PerfilResponse;
import com.tecsup.autosmart.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<PerfilResponse> obtener(Authentication auth) {
        return ResponseEntity.ok(usuarioService.obtenerPerfil(auth.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<PerfilResponse> actualizar(@Valid @RequestBody PerfilRequest request,
                                                     Authentication auth) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(auth.getName(), request));
    }
}