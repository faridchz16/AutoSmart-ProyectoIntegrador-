package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.comprobante.ComprobantePdf;
import com.tecsup.autosmart.service.ComprobanteService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*", exposedHeaders = HttpHeaders.CONTENT_DISPOSITION)
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    public ComprobanteController(ComprobanteService comprobanteService) {
        this.comprobanteService = comprobanteService;
    }

    @GetMapping("/payments/{id}/receipt")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id, Authentication auth) {
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        ComprobantePdf pdf = comprobanteService.generar(auth.getName(), esAdmin, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(pdf.nombreArchivo()).build().toString())
                .body(pdf.contenido());
    }
}