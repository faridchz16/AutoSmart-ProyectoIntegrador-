package com.tecsup.autosmart.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "fecha_bloqueada",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_mecanico", "fecha"}))
public class FechaBloqueada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloqueo")
    private Integer idBloqueo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mecanico", nullable = false)
    private Usuario mecanico;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "motivo", length = 200)
    private String motivo;

    public FechaBloqueada() {}

    public Integer getIdBloqueo() { return idBloqueo; }
    public void setIdBloqueo(Integer idBloqueo) { this.idBloqueo = idBloqueo; }
    public Usuario getMecanico() { return mecanico; }
    public void setMecanico(Usuario mecanico) { this.mecanico = mecanico; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}