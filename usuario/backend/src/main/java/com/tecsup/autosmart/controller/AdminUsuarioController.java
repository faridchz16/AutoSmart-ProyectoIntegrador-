package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.EstadoRequest;
import com.tecsup.autosmart.dto.PaginaResponse;
import com.tecsup.autosmart.dto.UsuarioResumenResponse;
import com.tecsup.autosmart.service.AdminUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@CrossOrigin(origins = "*")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    public AdminUsuarioController(AdminUsuarioService adminUsuarioService) {
        this.adminUsuarioService = adminUsuarioService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<UsuarioResumenResponse>> listar(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminUsuarioService.listar(rol, q, page, size));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResumenResponse> cambiarEstado(@PathVariable Integer id,
                                                                @Valid @RequestBody EstadoRequest request,
                                                                Authentication auth) {
        return ResponseEntity.ok(adminUsuarioService.cambiarEstado(id, request.getActivo(), auth.getName()));
    }
}