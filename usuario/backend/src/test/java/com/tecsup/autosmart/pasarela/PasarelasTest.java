package com.tecsup.autosmart.pasarela;

import com.tecsup.autosmart.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasarelasTest {

    private SolicitudCobro cobro(String token) {
        return new SolicitudCobro(new BigDecimal("150.00"), token, "AutoSmart prueba");
    }

    @Test
    void yapePlinApruebaConElCodigoDePrueba() {
        ResultadoPasarela resultado = new YapePlinSandboxPasarela().cobrar(cobro("123456"));

        assertTrue(resultado.aprobado());
        assertTrue(resultado.referencia().startsWith("YAPE-SANDBOX-"));
    }

    @Test
    void yapePlinRechazaOtroCodigo() {
        ResultadoPasarela resultado = new YapePlinSandboxPasarela().cobrar(cobro("000000"));

        assertFalse(resultado.aprobado());
        assertEquals("Codigo de aprobacion invalido", resultado.motivoRechazo());
    }

    @Test
    void tarjetaApruebaVisa() {
        ResultadoPasarela resultado = new TarjetaSandboxPasarela().cobrar(cobro("pm_card_visa"));

        assertTrue(resultado.aprobado());
        assertTrue(resultado.referencia().startsWith("TARJETA-SANDBOX-"));
    }

    @Test
    void tarjetaRechazaPorFondosInsuficientes() {
        ResultadoPasarela resultado = new TarjetaSandboxPasarela().cobrar(cobro("pm_card_chargeDeclinedInsufficientFunds"));

        assertFalse(resultado.aprobado());
        assertEquals("Fondos insuficientes", resultado.motivoRechazo());
    }

    @Test
    void tarjetaRechazaUnTokenDesconocido() {
        assertThrows(BadRequestException.class, () -> new TarjetaSandboxPasarela().cobrar(cobro("abc")));
    }
}