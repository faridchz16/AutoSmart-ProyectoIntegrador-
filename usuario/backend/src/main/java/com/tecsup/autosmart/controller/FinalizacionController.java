package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.FinalizacionResponse;
import com.tecsup.autosmart.service.FinalizacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class FinalizacionController {

    private final FinalizacionService finalizacionService;

    public FinalizacionController(FinalizacionService finalizacionService) {
        this.finalizacionService = finalizacionService;
    }

    @PutMapping("/work-orders/{id}/finish")
    public ResponseEntity<FinalizacionResponse> finalizar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(finalizacionService.finalizar(auth.getName(), id));
    }
}