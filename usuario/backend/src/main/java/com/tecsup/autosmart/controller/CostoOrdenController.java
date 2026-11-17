package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.CostoOrdenResponse;
import com.tecsup.autosmart.service.CostoOrdenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class CostoOrdenController {

    private final CostoOrdenService costoOrdenService;

    public CostoOrdenController(CostoOrdenService costoOrdenService) {
        this.costoOrdenService = costoOrdenService;
    }

    @GetMapping("/work-orders/{id}/cost")
    public ResponseEntity<CostoOrdenResponse> costo(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(costoOrdenService.costo(auth.getName(), id));
    }
}