package com.tecsup.autosmart.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_derivaciones")
public class Derivacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_derivacion")
    private Long idDerivacion;

    @Column(name = "id_conversacion", nullable = false, length = 36)
    private String idConversacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(length = 120)
    private String contacto;

    @Column(nullable = false, length = 2000)
    private String resumen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoDerivacion estado = EstadoDerivacion.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 500)
    private String nota;

    @Column(name = "atendido_por", length = 100)
    private String atendidoPor;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    public Long getIdDerivacion() { return idDerivacion; }
    public void setIdDerivacion(Long idDerivacion) { this.idDerivacion = idDerivacion; }
    public String getIdConversacion() { return idConversacion; }
    public void setIdConversacion(String idConversacion) { this.idConversacion = idConversacion; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public EstadoDerivacion getEstado() { return estado; }
    public void setEstado(EstadoDerivacion estado) { this.estado = estado; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
    public String getAtendidoPor() { return atendidoPor; }
    public void setAtendidoPor(String atendidoPor) { this.atendidoPor = atendidoPor; }
    public LocalDateTime getFechaAtencion() { return fechaAtencion; }
    public void setFechaAtencion(LocalDateTime fechaAtencion) { this.fechaAtencion = fechaAtencion; }
}