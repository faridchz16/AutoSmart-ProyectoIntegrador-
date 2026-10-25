package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.DiaTallerDto;
import com.tecsup.autosmart.dto.FeriadoRequest;
import com.tecsup.autosmart.dto.FeriadoResponse;
import com.tecsup.autosmart.service.ConfiguracionTallerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminTallerController {

    private final ConfiguracionTallerService tallerService;

    public AdminTallerController(ConfiguracionTallerService tallerService) {
        this.tallerService = tallerService;
    }

    @GetMapping("/shop-hours")
    public ResponseEntity<List<DiaTallerDto>> horario() {
        return ResponseEntity.ok(tallerService.listarHorario());
    }

    @PutMapping("/shop-hours")
    public ResponseEntity<List<DiaTallerDto>> actualizarHorario(@RequestBody List<DiaTallerDto> dias) {
        return ResponseEntity.ok(tallerService.actualizarHorario(dias));
    }

    @GetMapping("/holidays")
    public ResponseEntity<List<FeriadoResponse>> feriados(@RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(tallerService.listarFeriados(anio));
    }

    @PostMapping("/holidays")
    public ResponseEntity<FeriadoResponse> crearFeriado(@Valid @RequestBody FeriadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tallerService.crearFeriado(request));
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<Void> eliminarFeriado(@PathVariable Integer id) {
        tallerService.eliminarFeriado(id);
        return ResponseEntity.noContent().build();
    }
}