package com.tecsup.autosmart.reporte;

public interface ExportadorReporte {

    String formato();

    String contentType();

    byte[] exportar(Reporte reporte);
}