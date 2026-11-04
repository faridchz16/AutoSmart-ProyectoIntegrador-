package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.MaterialRequest;
import com.tecsup.autosmart.dto.MaterialResponse;
import com.tecsup.autosmart.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/materials")
@CrossOrigin(origins = "*")
public class AdminMaterialController {

    private final MaterialService materialService;

    public AdminMaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> crear(@Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.ok(materialService.actualizar(id, request));
    }

    @GetMapping
    public ResponseEntity<List<MaterialResponse>> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(defaultValue = "false") boolean soloBajoStock) {
        return ResponseEntity.ok(materialService.listar(buscar, soloBajoStock));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.obtener(id));
    }
}