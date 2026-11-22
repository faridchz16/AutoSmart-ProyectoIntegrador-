package com.tecsup.autosmart.comprobante;

import com.tecsup.autosmart.dto.CostoOrdenResponse;
import com.tecsup.autosmart.dto.CostoOrdenResponse.Adicional;
import com.tecsup.autosmart.dto.CostoOrdenResponse.RepuestoUtilizado;
import com.tecsup.autosmart.dto.CostoOrdenResponse.ServicioCobrado;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoPago;
import com.tecsup.autosmart.model.MetodoPago;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.Pago;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.model.Vehiculo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenPdfComprobanteGeneratorTest {

    private final OpenPdfComprobanteGenerator generador = new OpenPdfComprobanteGenerator();

    @Test
    void generaUnPdfValidoConAdicionalesYRepuestos() {
        CostoOrdenResponse costo = new CostoOrdenResponse(2L, 35L, "AS-2026-00035", "PAGADO",
                new ServicioCobrado("Cambio de aceite", new BigDecimal("120.00")),
                List.of(new Adicional("REPUESTO", "Filtro de aceite", 1, new BigDecimal("30.00"), new BigDecimal("30.00"))),
                new BigDecimal("30.00"),
                List.of(new RepuestoUtilizado("Aceite 20W-50", 4, "litros")),
                new BigDecimal("150.00"), "PEN", false, null);

        byte[] pdf = generador.generar(pago(true), costo);

        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void generaElPdfAunqueFaltenVehiculoMecanicoYAdicionales() {
        CostoOrdenResponse costo = new CostoOrdenResponse(2L, 35L, "AS-2026-00035", "PAGADO",
                new ServicioCobrado("Cambio de aceite", null),
                List.of(), BigDecimal.ZERO, List.of(),
                BigDecimal.ZERO, "PEN", false, null);

        byte[] pdf = generador.generar(pago(false), costo);

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    private Pago pago(boolean completo) {
        Usuario cliente = new Usuario();
        cliente.setNombre("Cliente Dos");
        Cita cita = new Cita();
        cita.setTicket("AS-2026-00035");
        cita.setCliente(cliente);
        if (completo) {
            Usuario mecanico = new Usuario();
            mecanico.setNombre("Mecanico Uno");
            cita.setMecanico(mecanico);
            Vehiculo vehiculo = new Vehiculo();
            vehiculo.setPlaca("ABC-123");
            vehiculo.setMarca("Toyota");
            vehiculo.setModelo("Yaris");
            cita.setVehiculo(vehiculo);
        }
        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setCita(cita);
        MetodoPago metodo = new MetodoPago();
        metodo.setCodigo("TARJETA");
        metodo.setNombre("Tarjeta de crédito o débito");
        Pago pago = new Pago();
        pago.setCodigo("PAG-2-00001");
        pago.setOrden(orden);
        pago.setMetodo(metodo);
        pago.setEstado(EstadoPago.APROBADO);
        pago.setMonto(new BigDecimal("150.00"));
        pago.setFecha(LocalDateTime.of(2026, 11, 21, 10, 30));
        return pago;
    }
}