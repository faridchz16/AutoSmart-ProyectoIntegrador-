package com.tecsup.autosmart.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_recuperacion")
public class CodigoRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_codigo")
    private Long idCodigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "codigo_hash", nullable = false, length = 100)
    private String codigoHash;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    private Boolean usado = false;

    @Column(nullable = false)
    private Integer intentos = 0;

    public CodigoRecuperacion() {}

    public Long getIdCodigo() { return idCodigo; }
    public void setIdCodigo(Long idCodigo) { this.idCodigo = idCodigo; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getCodigoHash() { return codigoHash; }
    public void setCodigoHash(String codigoHash) { this.codigoHash = codigoHash; }

    public LocalDateTime getExpiraEn() { return expiraEn; }
    public void setExpiraEn(LocalDateTime expiraEn) { this.expiraEn = expiraEn; }

    public Boolean getUsado() { return usado; }
    public void setUsado(Boolean usado) { this.usado = usado; }

    public Integer getIntentos() { return intentos; }
    public void setIntentos(Integer intentos) { this.intentos = intentos; }
}