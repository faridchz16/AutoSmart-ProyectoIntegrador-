package com.tecsup.autosmart.dto;

import java.util.List;

public class PaginaResponse<T> {

    private List<T> contenido;
    private int pagina;
    private int tamano;
    private long totalElementos;
    private int totalPaginas;

    public List<T> getContenido() { return contenido; }
    public void setContenido(List<T> contenido) { this.contenido = contenido; }

    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }

    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }

    public long getTotalElementos() { return totalElementos; }
    public void setTotalElementos(long totalElementos) { this.totalElementos = totalElementos; }

    public int getTotalPaginas() { return totalPaginas; }
    public void setTotalPaginas(int totalPaginas) { this.totalPaginas = totalPaginas; }
}