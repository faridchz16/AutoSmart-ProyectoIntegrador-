package com.tecsup.autosmart.dto;

import com.tecsup.autosmart.model.CategoriaDeficiencia;
import com.tecsup.autosmart.model.SeveridadDeficiencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DeficienciaRequest {

    @NotNull(message = "La categoria es obligatoria")
    private CategoriaDeficiencia categoria;

    @NotNull(message = "La severidad es obligatoria (LEVE, MODERADA o CRITICA)")
    private SeveridadDeficiencia severidad;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
    private String descripcion;

    public CategoriaDeficiencia getCategoria() { return categoria; }
    public void setCategoria(CategoriaDeficiencia categoria) { this.categoria = categoria; }
    public SeveridadDeficiencia getSeveridad() { return severidad; }
    public void setSeveridad(SeveridadDeficiencia severidad) { this.severidad = severidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}