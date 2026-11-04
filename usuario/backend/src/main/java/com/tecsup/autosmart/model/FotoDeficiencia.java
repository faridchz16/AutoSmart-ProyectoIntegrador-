package com.tecsup.autosmart.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fotos_deficiencia")
public class FotoDeficiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_foto")
    private Long idFoto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_deficiencia", nullable = false)
    private Deficiencia deficiencia;

    // Solo se guarda el nombre; la imagen esta en la carpeta app.fotos.directorio
    @Column(name = "nombre_archivo", nullable = false, unique = true, length = 100)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", nullable = false, length = 20)
    private String tipoContenido;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    public FotoDeficiencia() {}

    public Long getIdFoto() { return idFoto; }
    public void setIdFoto(Long idFoto) { this.idFoto = idFoto; }
    public Deficiencia getDeficiencia() { return deficiencia; }
    public void setDeficiencia(Deficiencia deficiencia) { this.deficiencia = deficiencia; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public String getTipoContenido() { return tipoContenido; }
    public void setTipoContenido(String tipoContenido) { this.tipoContenido = tipoContenido; }
    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}