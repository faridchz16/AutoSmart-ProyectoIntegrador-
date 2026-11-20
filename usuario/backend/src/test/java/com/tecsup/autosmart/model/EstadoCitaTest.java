package com.tecsup.autosmart.model;

import com.tecsup.autosmart.exception.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoCitaTest {

    @Test
    void flujoNormalDeUnaCita() {
        assertTrue(EstadoCita.PENDIENTE.puedeCambiarA(EstadoCita.CONFIRMADA));
        assertTrue(EstadoCita.CONFIRMADA.puedeCambiarA(EstadoCita.EN_PROCESO));
        assertTrue(EstadoCita.EN_PROCESO.puedeCambiarA(EstadoCita.ESPERA_APROBACION));
        assertTrue(EstadoCita.ESPERA_APROBACION.puedeCambiarA(EstadoCita.EN_PROCESO));
        assertTrue(EstadoCita.EN_PROCESO.puedeCambiarA(EstadoCita.FINALIZADA));
        assertTrue(EstadoCita.FINALIZADA.puedeCambiarA(EstadoCita.PAGADO));
    }

    @Test
    void cambiosNoPermitidos() {
        assertFalse(EstadoCita.PENDIENTE.puedeCambiarA(EstadoCita.PAGADO));
        assertFalse(EstadoCita.EN_PROCESO.puedeCambiarA(EstadoCita.PAGADO));
        assertFalse(EstadoCita.FINALIZADA.puedeCambiarA(EstadoCita.EN_PROCESO));
        assertFalse(EstadoCita.PAGADO.puedeCambiarA(EstadoCita.FINALIZADA));
        assertFalse(EstadoCita.CANCELADA.puedeCambiarA(EstadoCita.CONFIRMADA));
    }

    @Test
    void citaLanzaConflictoSiElCambioNoEstaPermitido() {
        Cita cita = new Cita();
        cita.setEstado(EstadoCita.EN_PROCESO);

        assertThrows(ConflictException.class, () -> cita.cambiarEstado(EstadoCita.PAGADO));
        assertEquals(EstadoCita.EN_PROCESO, cita.getEstado());
    }

    @Test
    void citaCambiaDeEstadoSiEstaPermitido() {
        Cita cita = new Cita();
        cita.setEstado(EstadoCita.FINALIZADA);

        cita.cambiarEstado(EstadoCita.PAGADO);

        assertEquals(EstadoCita.PAGADO, cita.getEstado());
    }
}