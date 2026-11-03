package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.ChecklistRequest;
import com.tecsup.autosmart.dto.ChecklistResponse;
import com.tecsup.autosmart.service.ChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/work-orders/{id}/checklist")
@CrossOrigin(origins = "*")
public class ChecklistController {

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping
    public ResponseEntity<ChecklistResponse> obtener(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(checklistService.obtener(auth.getName(), id));
    }

    @PutMapping
    public ResponseEntity<ChecklistResponse> guardar(@PathVariable Long id,
                                                     @Valid @RequestBody ChecklistRequest request,
                                                     Authentication auth) {
        return ResponseEntity.ok(checklistService.guardar(auth.getName(), id, request));
    }
}