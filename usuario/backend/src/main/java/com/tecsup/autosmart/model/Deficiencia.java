package com.tecsup.autosmart.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deficiencias")
public class Deficiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_deficiencia")
    private Long idDeficiencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenTrabajo orden;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, columnDefinition = "VARCHAR(20)")
    private CategoriaDeficiencia categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidad", nullable = false, columnDefinition = "VARCHAR(10)")
    private SeveridadDeficiencia severidad;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    public Deficiencia() {}

    public Long getIdDeficiencia() { return idDeficiencia; }
    public void setIdDeficiencia(Long idDeficiencia) { this.idDeficiencia = idDeficiencia; }
    public OrdenTrabajo getOrden() { return orden; }
    public void setOrden(OrdenTrabajo orden) { this.orden = orden; }
    public CategoriaDeficiencia getCategoria() { return categoria; }
    public void setCategoria(CategoriaDeficiencia categoria) { this.categoria = categoria; }
    public SeveridadDeficiencia getSeveridad() { return severidad; }
    public void setSeveridad(SeveridadDeficiencia severidad) { this.severidad = severidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}