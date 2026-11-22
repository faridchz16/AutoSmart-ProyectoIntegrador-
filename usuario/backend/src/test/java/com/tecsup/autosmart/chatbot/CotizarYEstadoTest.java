package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.dto.SeguimientoResponse;
import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.model.Vehiculo;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.service.SeguimientoService;
import com.tecsup.autosmart.service.ServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CotizarYEstadoTest {

    private CotizarManejador cotizar;
    private EstadoVehiculoManejador estado;
    private CitaRepository citaRepository;
    private SeguimientoService seguimientoService;
    private Usuario cliente;

    @BeforeEach
    void preparar() {
        ServicioService servicioService = mock(ServicioService.class);
        when(servicioService.listarActivos(null, null)).thenReturn(List.of(
                servicio(1L, "Cambio de aceite", "150"),
                servicio(2L, "Cambio de frenos", "220"),
                servicio(3L, "Alineación y balanceo", "90")));
        cotizar = new CotizarManejador(servicioService);

        citaRepository = mock(CitaRepository.class);
        seguimientoService = mock(SeguimientoService.class);
        estado = new EstadoVehiculoManejador(citaRepository, seguimientoService);

        cliente = new Usuario();
        cliente.setCorreo("cliente2@mail.com");
    }

    @Test
    void cotizaElServicioMencionadoConPrecioReferencial() {
        MensajeChat mensaje = MensajeChat.de("c", "¿Cuánto cuesta el cambio de ACEITE?", null);

        assertTrue(cotizar.aplica(mensaje));
        RespuestaChat res = cotizar.responder(mensaje);

        assertEquals("Cambio de aceite", res.datos().get("servicio"));
        assertEquals(new BigDecimal("150.00"), res.datos().get("precioDesde"));
        assertEquals(new BigDecimal("180.00"), res.datos().get("precioHasta"));
        assertEquals(true, res.datos().get("referencial"));
    }

    @Test
    void cotizaPorUnaSolaPalabraDelServicio() {
        RespuestaChat res = cotizar.responder(MensajeChat.de("c", "precio de la alineacion", null));

        assertEquals("Alineación y balanceo", res.datos().get("servicio"));
        assertEquals(new BigDecimal("108.00"), res.datos().get("precioHasta"));
    }

    @Test
    void siNoReconoceElServicioOfreceLasOpciones() {
        RespuestaChat res = cotizar.responder(MensajeChat.de("c", "cuanto cuesta", null));

        assertNull(res.datos());
        assertEquals("Precio de Cambio de aceite", res.opciones().get(0));
        assertEquals(3, res.opciones().size());
    }

    @Test
    void consultarEstadoRequiereSesionYDetectaLaPlaca() {
        assertTrue(estado.requiereSesion());
        assertTrue(estado.aplica(MensajeChat.de("c", "estado de DEF-456", null)));
        assertTrue(estado.aplica(MensajeChat.de("c", "def456", null)));
        assertFalse(estado.aplica(MensajeChat.de("c", "hola", null)));
    }

    @Test
    void respondeElEstadoYAvanceDelVehiculoEnReparacion() {
        Cita enProceso = cita(35L, "DEF-456", EstadoCita.EN_PROCESO, LocalDate.of(2026, 11, 22));
        Cita pagada = cita(20L, "DEF-456", EstadoCita.PAGADO, LocalDate.of(2026, 11, 1));
        when(citaRepository.buscarDelCliente(cliente)).thenReturn(List.of(pagada, enProceso));
        when(seguimientoService.seguimiento("cliente2@mail.com", 35L)).thenReturn(seguimiento("DEF-456", "EN_PROCESO", 60));

        RespuestaChat res = estado.responder(MensajeChat.de("c", "¿cómo va mi auto?", cliente));

        assertEquals("EN_PROCESO", res.datos().get("estado"));
        assertEquals(60, res.datos().get("avance"));
        assertTrue(res.respuesta().contains("Avance: 60 %"));
    }

    @Test
    void conVariosVehiculosFiltraPorLaPlacaYOfreceLosDemas() {
        Cita deUno = cita(35L, "DEF-456", EstadoCita.EN_PROCESO, LocalDate.of(2026, 11, 22));
        Cita deOtro = cita(36L, "ABC-123", EstadoCita.CONFIRMADA, LocalDate.of(2026, 11, 25));
        when(citaRepository.buscarDelCliente(cliente)).thenReturn(List.of(deUno, deOtro));
        when(seguimientoService.seguimiento(anyString(), anyLong())).thenReturn(seguimiento("ABC-123", "CONFIRMADA", 0));

        RespuestaChat porPlaca = estado.responder(MensajeChat.de("c", "estado de abc-123", cliente));
        assertTrue(porPlaca.respuesta().contains("confirmada para el 25/11"));

        RespuestaChat general = estado.responder(MensajeChat.de("c", "estado", cliente));
        assertEquals(List.of("Estado de DEF-456"), general.opciones());
    }

    @Test
    void sinServiciosEnCursoLoIndica() {
        when(citaRepository.buscarDelCliente(cliente)).thenReturn(List.of(
                cita(20L, "DEF-456", EstadoCita.PAGADO, LocalDate.of(2026, 11, 1))));

        RespuestaChat res = estado.responder(MensajeChat.de("c", "estado", cliente));

        assertEquals("No tienes servicios en curso en este momento.", res.respuesta());
    }

    private ServicioResponse servicio(Long id, String nombre, String precio) {
        ServicioResponse s = new ServicioResponse();
        s.setIdServicio(id);
        s.setNombre(nombre);
        s.setPrecioBase(new BigDecimal(precio));
        return s;
    }

    private Cita cita(Long id, String placa, EstadoCita estadoCita, LocalDate fecha) {
        Vehiculo v = new Vehiculo();
        v.setPlaca(placa);
        Cita c = new Cita();
        c.setIdCita(id);
        c.setVehiculo(v);
        c.setEstado(estadoCita);
        c.setFecha(fecha);
        c.setHoraInicio(LocalTime.of(10, 0));
        return c;
    }

    private SeguimientoResponse seguimiento(String placa, String estadoCita, int avance) {
        SeguimientoResponse s = new SeguimientoResponse();
        s.setTicket("AS-2026-00035");
        s.setPlaca(placa);
        s.setEstado(estadoCita);
        s.setPorcentajeAvance(avance);
        return s;
    }
}