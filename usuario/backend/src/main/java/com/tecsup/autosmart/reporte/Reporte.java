package com.tecsup.autosmart.reporte;

import java.util.List;

public record Reporte(
        String titulo,
        String nombreArchivo,
        List<String> columnas,
        List<List<Object>> filas,
        List<String> resumen
) {
}