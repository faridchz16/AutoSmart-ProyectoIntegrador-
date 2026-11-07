package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.TareaEstadoRequest;
import com.tecsup.autosmart.dto.TareaRequest;
import com.tecsup.autosmart.dto.TareasOrdenResponse;
import com.tecsup.autosmart.service.TareaOrdenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/work-orders/{id}/tasks")
@CrossOrigin(origins = "*")
public class TareaOrdenController {

    private final TareaOrdenService tareaService;

    public TareaOrdenController(TareaOrdenService tareaService) {
        this.tareaService = tareaService;
    }

    @PostMapping
    public ResponseEntity<TareasOrdenResponse> agregar(@PathVariable Long id,
                                                       @Valid @RequestBody TareaRequest request,
                                                       Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tareaService.agregar(auth.getName(), id, request));
    }

    @GetMapping
    public ResponseEntity<TareasOrdenResponse> listar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(tareaService.listar(auth.getName(), id));
    }

    @PutMapping("/{idTarea}")
    public ResponseEntity<TareasOrdenResponse> cambiarEstado(@PathVariable Long id,
                                                             @PathVariable Long idTarea,
                                                             @Valid @RequestBody TareaEstadoRequest request,
                                                             Authentication auth) {
        return ResponseEntity.ok(tareaService.cambiarEstado(auth.getName(), id, idTarea, request));
    }
}