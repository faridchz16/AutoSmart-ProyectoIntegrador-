package com.tecsup.autosmart.dto;

import java.util.ArrayList;
import java.util.List;

public class DeficienciaResponse {

    private Long idDeficiencia;
    private Long idOrden;
    private String categoria;
    private String severidad;
    private String descripcion;
    private String fechaRegistro;
    private List<Foto> fotos = new ArrayList<>();

    public Long getIdDeficiencia() { return idDeficiencia; }
    public void setIdDeficiencia(Long idDeficiencia) { this.idDeficiencia = idDeficiencia; }
    public Long getIdOrden() { return idOrden; }
    public void setIdOrden(Long idOrden) { this.idOrden = idOrden; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public List<Foto> getFotos() { return fotos; }
    public void setFotos(List<Foto> fotos) { this.fotos = fotos; }

    public static class Foto {
        private Long idFoto;
        private String url;

        public Foto(Long idFoto, String url) {
            this.idFoto = idFoto;
            this.url = url;
        }

        public Long getIdFoto() { return idFoto; }
        public String getUrl() { return url; }
    }
}