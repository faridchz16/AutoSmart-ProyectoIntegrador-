package com.tecsup.autosmart.dto;

import com.tecsup.autosmart.model.EstadoPresupuesto;
import jakarta.validation.constraints.NotNull;

public class DecisionPresupuestoRequest {

    @NotNull(message = "La decision es obligatoria (APROBADO o RECHAZADO)")
    private EstadoPresupuesto decision;

    public EstadoPresupuesto getDecision() { return decision; }
    public void setDecision(EstadoPresupuesto decision) { this.decision = decision; }
}