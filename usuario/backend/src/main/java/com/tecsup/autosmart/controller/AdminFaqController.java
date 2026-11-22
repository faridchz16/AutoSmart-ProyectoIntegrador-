package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.FaqRequest;
import com.tecsup.autosmart.dto.FaqResponse;
import com.tecsup.autosmart.service.FaqService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/admin/faq")
public class AdminFaqController {

    private final FaqService faqService;

    public AdminFaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    @GetMapping
    public ResponseEntity<List<FaqResponse>> listar() {
        return ResponseEntity.ok(faqService.listar());
    }

    @PostMapping
    public ResponseEntity<FaqResponse> crear(@Valid @RequestBody FaqRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(faqService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FaqResponse> actualizar(@PathVariable Integer id, @Valid @RequestBody FaqRequest request) {
        return ResponseEntity.ok(faqService.actualizar(id, request));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<FaqResponse> desactivar(@PathVariable Integer id) {
        return ResponseEntity.ok(faqService.desactivar(id));
    }
}