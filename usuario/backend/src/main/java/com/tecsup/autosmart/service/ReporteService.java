package com.tecsup.autosmart.service;

import com.tecsup.autosmart.exception.BadRequestException;
import com.tecsup.autosmart.reporte.ArchivoReporte;
import com.tecsup.autosmart.reporte.ExportadorReporte;
import com.tecsup.autosmart.reporte.FuenteReporte;
import com.tecsup.autosmart.reporte.Reporte;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final Map<String, FuenteReporte> fuentes;
    private final Map<String, ExportadorReporte> exportadores;

    public ReporteService(List<FuenteReporte> fuentes, List<ExportadorReporte> exportadores) {
        this.fuentes = fuentes.stream().collect(Collectors.toMap(FuenteReporte::tipo, Function.identity()));
        this.exportadores = exportadores.stream().collect(Collectors.toMap(ExportadorReporte::formato, Function.identity()));
    }

    public ArchivoReporte generar(String tipo, String formato, Map<String, String> filtros) {
        FuenteReporte fuente = fuentes.get(tipo == null ? "" : tipo.trim().toLowerCase(Locale.ROOT));
        if (fuente == null) {
            throw new BadRequestException("Tipo de reporte invalido. Usa: " + String.join(", ", fuentes.keySet().stream().sorted().toList()));
        }
        String clave = formato == null || formato.isBlank() ? "xlsx" : formato.trim().toLowerCase(Locale.ROOT);
        ExportadorReporte exportador = exportadores.get(clave);
        if (exportador == null) {
            throw new BadRequestException("Formato invalido. Usa: " + String.join(", ", exportadores.keySet().stream().sorted().toList()));
        }

        Reporte reporte = fuente.generar(filtros);
        return new ArchivoReporte(reporte.nombreArchivo() + "." + exportador.formato(),
                exportador.contentType(), exportador.exportar(reporte));
    }
}