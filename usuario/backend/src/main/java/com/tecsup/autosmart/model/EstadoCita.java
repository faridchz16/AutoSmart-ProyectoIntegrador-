package com.tecsup.autosmart.model;

import java.util.Set;

public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    RECHAZADA,
    CANCELADA,
    EN_PROCESO,
    ESPERA_APROBACION,
    FINALIZADA,
    PAGADO;

    public boolean puedeCambiarA(EstadoCita destino) {
        return switch (this) {
            case PENDIENTE -> Set.of(PENDIENTE, CONFIRMADA, RECHAZADA, CANCELADA).contains(destino);
            case CONFIRMADA -> Set.of(PENDIENTE, EN_PROCESO, CANCELADA).contains(destino);
            case EN_PROCESO -> Set.of(ESPERA_APROBACION, FINALIZADA).contains(destino);
            case ESPERA_APROBACION -> destino == EN_PROCESO;
            case FINALIZADA -> destino == PAGADO;
            case RECHAZADA, CANCELADA, PAGADO -> false;
        };
    }

    public boolean terminada() {
        return this == FINALIZADA || this == PAGADO;
    }
}