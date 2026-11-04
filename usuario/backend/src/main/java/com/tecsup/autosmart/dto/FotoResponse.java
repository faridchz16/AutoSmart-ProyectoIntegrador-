package com.tecsup.autosmart.dto;

public class FotoResponse {

    private Long idFoto;
    private Long idDeficiencia;
    private String url;

    public FotoResponse(Long idFoto, Long idDeficiencia, String url) {
        this.idFoto = idFoto;
        this.idDeficiencia = idDeficiencia;
        this.url = url;
    }

    public Long getIdFoto() { return idFoto; }
    public Long getIdDeficiencia() { return idDeficiencia; }
    public String getUrl() { return url; }
}