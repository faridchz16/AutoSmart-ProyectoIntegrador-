package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.HorarioRequest;
import com.tecsup.autosmart.dto.HorarioResponse;
import com.tecsup.autosmart.service.HorarioMecanicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mechanics/me/schedule")
@CrossOrigin(origins = "*")
public class HorarioMecanicoController {

    private final HorarioMecanicoService horarioService;

    public HorarioMecanicoController(HorarioMecanicoService horarioService) {
        this.horarioService = horarioService;
    }

    @GetMapping
    public ResponseEntity<List<HorarioResponse>> listar(Authentication auth) {
        return ResponseEntity.ok(horarioService.listar(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<HorarioResponse> crear(@Valid @RequestBody HorarioRequest request,
                                                 Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horarioService.crear(auth.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HorarioResponse> actualizar(@PathVariable Integer id,
                                                      @Valid @RequestBody HorarioRequest request,
                                                      Authentication auth) {
        return ResponseEntity.ok(horarioService.actualizar(auth.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, Authentication auth) {
        horarioService.eliminar(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}