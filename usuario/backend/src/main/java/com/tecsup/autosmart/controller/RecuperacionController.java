package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.ForgotPasswordRequest;
import com.tecsup.autosmart.dto.ResetPasswordRequest;
import com.tecsup.autosmart.service.RecuperacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class RecuperacionController {

    private final RecuperacionService recuperacionService;

    public RecuperacionController(RecuperacionService recuperacionService) {
        this.recuperacionService = recuperacionService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> solicitar(@Valid @RequestBody ForgotPasswordRequest request) {
        recuperacionService.solicitarCodigo(request.getCorreo());
        return ResponseEntity.ok(Map.of("mensaje",
                "Si el correo esta registrado, recibiras un codigo para restablecer tu contrasena"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> restablecer(@Valid @RequestBody ResetPasswordRequest request) {
        recuperacionService.restablecer(request);
        return ResponseEntity.ok(Map.of("mensaje", "Contrasena actualizada. Ya puedes iniciar sesion"));
    }
}