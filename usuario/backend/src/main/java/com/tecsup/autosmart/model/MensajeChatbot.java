package com.tecsup.autosmart.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_mensajes")
public class MensajeChatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long idMensaje;

    @Column(name = "id_conversacion", nullable = false, length = 36)
    private String idConversacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EmisorChat emisor;

    @Column(nullable = false, length = 1000)
    private String texto;

    @Column(length = 30)
    private String intencion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public MensajeChatbot() {
    }

    public MensajeChatbot(String idConversacion, Usuario usuario, EmisorChat emisor, String texto,
                          String intencion, LocalDateTime fecha) {
        this.idConversacion = idConversacion;
        this.usuario = usuario;
        this.emisor = emisor;
        this.texto = texto;
        this.intencion = intencion;
        this.fecha = fecha;
    }

    public Long getIdMensaje() { return idMensaje; }
    public String getIdConversacion() { return idConversacion; }
    public Usuario getUsuario() { return usuario; }
    public EmisorChat getEmisor() { return emisor; }
    public String getTexto() { return texto; }
    public String getIntencion() { return intencion; }
    public LocalDateTime getFecha() { return fecha; }
}