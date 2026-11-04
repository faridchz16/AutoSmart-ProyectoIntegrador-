package com.tecsup.autosmart.dto;

public class DiagnosticoResponse {

    private Long idOrden;
    private String diagnostico;
    private String fechaDiagnostico;

    public DiagnosticoResponse(Long idOrden, String diagnostico, String fechaDiagnostico) {
        this.idOrden = idOrden;
        this.diagnostico = diagnostico;
        this.fechaDiagnostico = fechaDiagnostico;
    }

    public Long getIdOrden() { return idOrden; }
    public String getDiagnostico() { return diagnostico; }
    public String getFechaDiagnostico() { return fechaDiagnostico; }
}