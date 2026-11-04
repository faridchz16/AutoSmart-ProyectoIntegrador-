package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.FotoResponse;
import com.tecsup.autosmart.model.FotoDeficiencia;
import com.tecsup.autosmart.service.FotoDeficienciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "*")
public class FotoDeficienciaController {

    private final FotoDeficienciaService fotoService;

    public FotoDeficienciaController(FotoDeficienciaService fotoService) {
        this.fotoService = fotoService;
    }

    @PostMapping(value = "/work-orders/{id}/deficiencies/{idDeficiencia}/photos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoResponse> subir(@PathVariable Long id,
                                              @PathVariable Long idDeficiencia,
                                              @RequestParam(value = "foto", required = false) MultipartFile foto,
                                              Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fotoService.subir(auth.getName(), id, idDeficiencia, foto));
    }

    @GetMapping("/photos/{idFoto}")
    public ResponseEntity<byte[]> ver(@PathVariable Long idFoto, Authentication auth) {
        FotoDeficiencia foto = fotoService.buscarParaVer(auth.getName(), idFoto);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipoContenido()))
                .body(fotoService.leerArchivo(foto));
    }
}