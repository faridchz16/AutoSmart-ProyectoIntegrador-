package com.tecsup.autosmart.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordenes_trabajo")
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Long idOrden;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita", nullable = false, unique = true)
    private Cita cita;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "diagnostico", length = 2000)
    private String diagnostico;

    @Column(name = "fecha_diagnostico")
    private LocalDateTime fechaDiagnostico;

    @Column(name = "costo_repuestos", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoRepuestos = BigDecimal.ZERO;

    public OrdenTrabajo() {}

    public Long getIdOrden() { return idOrden; }
    public void setIdOrden(Long idOrden) { this.idOrden = idOrden; }
    public Cita getCita() { return cita; }
    public void setCita(Cita cita) { this.cita = cita; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public LocalDateTime getFechaDiagnostico() { return fechaDiagnostico; }
    public void setFechaDiagnostico(LocalDateTime fechaDiagnostico) { this.fechaDiagnostico = fechaDiagnostico; }
    public BigDecimal getCostoRepuestos() { return costoRepuestos; }
    public void setCostoRepuestos(BigDecimal costoRepuestos) { this.costoRepuestos = costoRepuestos; }
}