package com.tecsup.autosmart.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "calificaciones")
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_calificacion")
    private Long idCalificacion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden", nullable = false, unique = true)
    private OrdenTrabajo orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mecanico", nullable = false)
    private Usuario mecanico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Usuario cliente;

    @Column(nullable = false)
    private Integer estrellas;

    @Column(length = 500)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private Boolean oculto = false;

    @Column(name = "motivo_ocultado", length = 200)
    private String motivoOcultado;

    @Column(name = "ocultado_por", length = 100)
    private String ocultadoPor;

    @Column(name = "fecha_ocultado")
    private LocalDateTime fechaOcultado;

    public Long getIdCalificacion() { return idCalificacion; }
    public OrdenTrabajo getOrden() { return orden; }
    public void setOrden(OrdenTrabajo orden) { this.orden = orden; }
    public Usuario getMecanico() { return mecanico; }
    public void setMecanico(Usuario mecanico) { this.mecanico = mecanico; }
    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }
    public Integer getEstrellas() { return estrellas; }
    public void setEstrellas(Integer estrellas) { this.estrellas = estrellas; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Boolean getOculto() { return oculto; }
    public void setOculto(Boolean oculto) { this.oculto = oculto; }
    public String getMotivoOcultado() { return motivoOcultado; }
    public void setMotivoOcultado(String motivoOcultado) { this.motivoOcultado = motivoOcultado; }
    public String getOcultadoPor() { return ocultadoPor; }
    public void setOcultadoPor(String ocultadoPor) { this.ocultadoPor = ocultadoPor; }
    public LocalDateTime getFechaOcultado() { return fechaOcultado; }
    public void setFechaOcultado(LocalDateTime fechaOcultado) { this.fechaOcultado = fechaOcultado; }
}