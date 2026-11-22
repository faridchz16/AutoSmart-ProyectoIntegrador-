package com.tecsup.autosmart.model;

import com.tecsup.autosmart.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventarioDominioTest {

    private Material material(int stock, String costo) {
        Material m = new Material();
        m.setNombre("Filtro de aceite");
        m.setStockActual(stock);
        m.setStockMinimo(2);
        m.setCostoUnitario(new BigDecimal(costo));
        return m;
    }

    @Test
    void descontarBajaElStockYDevuelveElCostoDeLaSalida() {
        Material filtro = material(10, "25.50");

        BigDecimal costo = filtro.descontar(3);

        assertEquals(7, filtro.getStockActual());
        assertEquals(new BigDecimal("76.50"), costo);
    }

    @Test
    void noPermiteDescontarMasDeLoQueHayYNoTocaElStock() {
        Material filtro = material(2, "25.50");

        assertThrows(ConflictException.class, () -> filtro.descontar(5));
        assertEquals(2, filtro.getStockActual());
    }

    @Test
    void ingresarSumaAlStock() {
        Material filtro = material(2, "25.50");

        filtro.ingresar(8);

        assertEquals(10, filtro.getStockActual());
    }

    @Test
    void laOrdenAcumulaElCostoDeRepuestosAunqueEmpieceEnNulo() {
        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setCostoRepuestos(null);

        orden.sumarCostoRepuestos(new BigDecimal("76.50"));
        orden.sumarCostoRepuestos(new BigDecimal("30.00"));
        orden.sumarCostoRepuestos(null);

        assertEquals(new BigDecimal("106.50"), orden.getCostoRepuestos());
    }
}