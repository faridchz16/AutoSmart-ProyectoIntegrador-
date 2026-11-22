package com.tecsup.autosmart.reporte;

import java.util.Map;

public interface FuenteReporte {

    String tipo();

    Reporte generar(Map<String, String> filtros);
}