package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.CitaAgendaResponse;
import com.tecsup.autosmart.service.AgendaMecanicoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/mechanics/me/appointments")
@CrossOrigin(origins = "*")
public class AgendaMecanicoController {

    private final AgendaMecanicoService agendaService;

    public AgendaMecanicoController(AgendaMecanicoService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping
    public ResponseEntity<List<CitaAgendaResponse>> agenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication auth) {
        return ResponseEntity.ok(agendaService.agenda(auth.getName(), desde, hasta));
    }
}