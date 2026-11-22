package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.dto.CitaCreadaResponse;
import com.tecsup.autosmart.dto.CitaRequest;
import com.tecsup.autosmart.dto.MecanicoDisponibleResponse;
import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.dto.TurnoResponse;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.model.Vehiculo;
import com.tecsup.autosmart.repository.VehiculoRepository;
import com.tecsup.autosmart.service.CitaService;
import com.tecsup.autosmart.service.DisponibilidadService;
import com.tecsup.autosmart.service.ServicioService;
import com.tecsup.autosmart.service.TurnoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgendarManejadorTest {

    private static final String CONVERSACION = "c7f1e2a0-5b1d-4c2e-9f00-1a2b3c4d5e6f";

    private VehiculoRepository vehiculoRepository;
    private CitaService citaService;
    private AlmacenBorradores almacen;
    private AgendarManejador manejador;
    private Usuario cliente;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-22T20:00:00Z"), ZoneId.of("America/Lima"));
        almacen = new AlmacenBorradoresMemoria(reloj);

        ServicioService servicioService = mock(ServicioService.class);
        ServicioResponse aceite = new ServicioResponse();
        aceite.setIdServicio(1L);
        aceite.setNombre("Cambio de aceite");
        aceite.setPrecioBase(new BigDecimal("150"));
        when(servicioService.listarActivos(null, null)).thenReturn(List.of(aceite));

        DisponibilidadService disponibilidad = mock(DisponibilidadService.class);
        MecanicoDisponibleResponse mecanico = new MecanicoDisponibleResponse();
        mecanico.setIdMecanico(7);
        mecanico.setNombre("Mecanico Uno");
        when(disponibilidad.mecanicosPorServicio(1L)).thenReturn(List.of(mecanico));

        TurnoService turnoService = mock(TurnoService.class);
        when(turnoService.turnosLibres(eq(1L), eq(7), any(LocalDate.class), anyInt())).thenReturn(List.of(
                new TurnoResponse(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                new TurnoResponse(LocalTime.of(11, 0), LocalTime.of(12, 0))));

        vehiculoRepository = mock(VehiculoRepository.class);
        citaService = mock(CitaService.class);

        cliente = new Usuario();
        cliente.setCorreo("cliente2@mail.com");
        cliente.setIdUsuario(3);
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setIdVehiculo(9L);
        vehiculo.setPlaca("DEF-456");
        when(vehiculoRepository.findByClienteAndActivoTrue(cliente)).thenReturn(List.of(vehiculo));

        manejador = new AgendarManejador(almacen, servicioService, vehiculoRepository, disponibilidad,
                turnoService, citaService, reloj);
    }

    private RespuestaChat enviar(String texto) {
        MensajeChat mensaje = MensajeChat.de(CONVERSACION, texto, cliente);
        assertTrue(manejador.aplica(mensaje));
        return manejador.responder(mensaje);
    }

    @Test
    void completaElFlujoYCreaLaCitaConLaMismaLogicaDeLaApp() {
        CitaCreadaResponse creada = new CitaCreadaResponse();
        creada.setIdCita(41L);
        creada.setTicket("AS-2026-00041");
        creada.setEstado("PENDIENTE");
        when(citaService.reservar(eq("cliente2@mail.com"), any(CitaRequest.class))).thenReturn(creada);

        RespuestaChat paso1 = enviar("Quiero agendar un cambio de aceite");
        assertTrue(paso1.respuesta().contains("Usaré tu vehículo DEF-456"));
        assertEquals("Dom 22/11", paso1.opciones().get(0));

        RespuestaChat paso2 = enviar("mañana");
        assertEquals(List.of("10:00", "11:00", "Cancelar"), paso2.opciones());

        RespuestaChat paso3 = enviar("10:00");
        assertEquals(List.of("Confirmar", "Cancelar"), paso3.opciones());
        assertEquals("Mecanico Uno", paso3.datos().get("mecanico"));

        RespuestaChat fin = enviar("Confirmar");
        assertEquals("AS-2026-00041", fin.datos().get("ticket"));
        assertTrue(almacen.obtener(CONVERSACION).isEmpty());

        ArgumentCaptor<CitaRequest> captor = ArgumentCaptor.forClass(CitaRequest.class);
        verify(citaService).reservar(eq("cliente2@mail.com"), captor.capture());
        assertEquals(1L, captor.getValue().getIdServicio());
        assertEquals(7, captor.getValue().getIdMecanico());
        assertEquals(9L, captor.getValue().getIdVehiculo());
        assertEquals(LocalDate.of(2026, 11, 23), captor.getValue().getFecha());
        assertEquals(LocalTime.of(10, 0), captor.getValue().getHoraInicio());
    }

    @Test
    void siElTurnoSeOcupaVuelveAOfrecerHorarios() {
        when(citaService.reservar(any(), any())).thenThrow(new ConflictException("El turno ya no esta disponible"));
        enviar("agendar cambio de aceite");
        enviar("23/11");
        enviar("10:00");

        RespuestaChat res = enviar("si");

        assertTrue(res.respuesta().startsWith("Ese turno se acaba de ocupar"));
        assertEquals(PasoAgendar.TURNO, almacen.obtener(CONVERSACION).orElseThrow().getPaso());
    }

    @Test
    void cancelarTerminaElFlujoSinCrearNada() {
        enviar("quiero una cita");
        RespuestaChat res = enviar("Cancelar");

        assertTrue(res.respuesta().startsWith("Listo, cancelé"));
        assertTrue(almacen.obtener(CONVERSACION).isEmpty());
        verify(citaService, never()).reservar(any(), any());
    }

    @Test
    void sinVehiculosNoInicia() {
        when(vehiculoRepository.findByClienteAndActivoTrue(cliente)).thenReturn(List.of());

        RespuestaChat res = enviar("agendar cambio de aceite");

        assertTrue(res.respuesta().startsWith("No tienes vehículos"));
        assertTrue(almacen.obtener(CONVERSACION).isEmpty());
    }

    @Test
    void pideElServicioSiNoLoReconoceYRequiereSesion() {
        assertTrue(manejador.requiereSesion());
        RespuestaChat res = enviar("quiero reservar");

        assertEquals(List.of("Cambio de aceite", "Cancelar"), res.opciones());
        assertFalse(manejador.aplica(MensajeChat.de("otra", "hola", cliente)));
    }

    @Test
    void leeLaHoraEnVariosFormatos() {
        assertEquals(LocalTime.of(10, 0), AgendarManejador.leerHora("10:00"));
        assertEquals(LocalTime.of(15, 0), AgendarManejador.leerHora("3 pm"));
        assertEquals(LocalTime.of(9, 30), AgendarManejador.leerHora("a las 9:30"));
        assertEquals(LocalTime.of(11, 0), AgendarManejador.leerHora("11"));
        assertNull(AgendarManejador.leerHora("temprano"));
    }

    @Test
    void elBorradorExpiraALos30Minutos() {
        MutableClock reloj = new MutableClock(Instant.parse("2026-11-22T20:00:00Z"));
        AlmacenBorradoresMemoria memoria = new AlmacenBorradoresMemoria(reloj);
        memoria.guardar("x", new BorradorCita());

        reloj.avanzarMinutos(29);
        assertTrue(memoria.obtener("x").isPresent());
        reloj.avanzarMinutos(2);
        assertTrue(memoria.obtener("x").isEmpty());
    }

    private static class MutableClock extends Clock {
        private Instant ahora;

        MutableClock(Instant ahora) {
            this.ahora = ahora;
        }

        void avanzarMinutos(long minutos) {
            ahora = ahora.plusSeconds(minutos * 60);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Lima");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}