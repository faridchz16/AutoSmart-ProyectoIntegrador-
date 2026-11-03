package com.tecsup.autosmart.model;

import jakarta.persistence.*;

@Entity
@Table(name = "checklist_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_checklist", "elemento"}))
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long idItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_checklist", nullable = false)
    private Checklist checklist;

    @Enumerated(EnumType.STRING)
    @Column(name = "elemento", nullable = false, columnDefinition = "VARCHAR(20)")
    private ElementoChecklist elemento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "VARCHAR(10)")
    private EstadoItemChecklist estado;

    @Column(name = "observacion", length = 300)
    private String observacion;

    public ChecklistItem() {}

    public Long getIdItem() { return idItem; }
    public void setIdItem(Long idItem) { this.idItem = idItem; }
    public Checklist getChecklist() { return checklist; }
    public void setChecklist(Checklist checklist) { this.checklist = checklist; }
    public ElementoChecklist getElemento() { return elemento; }
    public void setElemento(ElementoChecklist elemento) { this.elemento = elemento; }
    public EstadoItemChecklist getEstado() { return estado; }
    public void setEstado(EstadoItemChecklist estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}