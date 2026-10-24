package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.CitaCreadaResponse;
import com.tecsup.autosmart.dto.CitaDetalleResponse;
import com.tecsup.autosmart.dto.CitaRequest;
import com.tecsup.autosmart.service.CitaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.tecsup.autosmart.dto.RechazoRequest;

@RestController
@RequestMapping("/appointments")
@CrossOrigin(origins = "*")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping
    public ResponseEntity<CitaCreadaResponse> reservar(@Valid @RequestBody CitaRequest request,
                                                       Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(citaService.reservar(auth.getName(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CitaDetalleResponse> detalle(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(citaService.detalleParaMecanico(auth.getName(), id));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<CitaCreadaResponse>> mias(
            @RequestParam(defaultValue = "false") boolean futuras, Authentication auth) {
        return ResponseEntity.ok(citaService.misCitas(auth.getName(), futuras));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<CitaCreadaResponse> aceptar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(citaService.aceptar(auth.getName(), id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<CitaCreadaResponse> rechazar(@PathVariable Long id,
                                                       @Valid @RequestBody RechazoRequest request,
                                                       Authentication auth) {
        return ResponseEntity.ok(citaService.rechazar(auth.getName(), id, request.getMotivo()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<CitaCreadaResponse> cancelar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(citaService.cancelar(auth.getName(), id));
    }
}