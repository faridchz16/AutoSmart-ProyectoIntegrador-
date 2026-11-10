package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.FcmTokenRequest;
import com.tecsup.autosmart.service.FcmTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me/fcm-token")
@CrossOrigin(origins = "*")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    public FcmTokenController(FcmTokenService fcmTokenService) {
        this.fcmTokenService = fcmTokenService;
    }

    @PutMapping
    public ResponseEntity<Void> registrar(@Valid @RequestBody FcmTokenRequest request, Authentication auth) {
        fcmTokenService.registrar(auth.getName(), request.getToken());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(Authentication auth) {
        fcmTokenService.eliminar(auth.getName());
        return ResponseEntity.noContent().build();
    }
}