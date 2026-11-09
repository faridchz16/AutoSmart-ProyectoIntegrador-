package com.tecsup.autosmart.dto;

import com.tecsup.autosmart.model.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DecisionSolicitudRequest {

    @NotNull(message = "La decision es obligatoria (APROBADA o RECHAZADA)")
    private EstadoSolicitud decision;

    @Size(max = 200, message = "La observacion no puede superar 200 caracteres")
    private String observacion;

    public EstadoSolicitud getDecision() { return decision; }
    public void setDecision(EstadoSolicitud decision) { this.decision = decision; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}