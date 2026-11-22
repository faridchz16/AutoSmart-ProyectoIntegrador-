package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.HistorialVehiculoResponse;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.OrdenTrabajo;
import com.tecsup.autosmart.model.Servicio;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.model.Vehiculo;
import com.tecsup.autosmart.repository.OrdenTrabajoRepository;
import com.tecsup.autosmart.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HistorialVehiculoServiceTest {

    private VehiculoRepository vehiculoRepository;
    private OrdenTrabajoRepository ordenRepository;
    private HistorialVehiculoService service;
    private Vehiculo vehiculo;

    @BeforeEach
    void preparar() {
        vehiculoRepository = mock(VehiculoRepository.class);
        ordenRepository = mock(OrdenTrabajoRepository.class);
        service = new HistorialVehiculoService(vehiculoRepository, ordenRepository);

        Usuario cliente = new Usuario();
        cliente.setNombre("Cliente Prueba");
        cliente.setCorreo("cliente2@mail.com");
        vehiculo = new Vehiculo();
        vehiculo.setIdVehiculo(7L);
        vehiculo.setPlaca("DEF-456");
        vehiculo.setMarca("Toyota");
        vehiculo.setModelo("Yaris");
        vehiculo.setCliente(cliente);

        Usuario mecanico = new Usuario();
        mecanico.setNombre("Mecanico");
        Servicio servicio = new Servicio();
        servicio.setNombre("Cambio de aceite");
        Cita cita = new Cita();
        cita.setTicket("AS-2026-00035");
        cita.setEstado(EstadoCita.PAGADO);
        cita.setServicio(servicio);
        cita.setMecanico(mecanico);
        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setIdOrden(2L);
        orden.setCita(cita);
        orden.setFechaInicio(LocalDateTime.of(2026, 11, 9, 8, 5));

        when(vehiculoRepository.findByPlacaIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(vehiculoRepository.findByPlacaIgnoreCase("def-456")).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.findById(7L)).thenReturn(Optional.of(vehiculo));
        when(ordenRepository.historialDelVehiculo(7L)).thenReturn(List.of(orden));
    }

    @Test
    void elAdminBuscaPorPlacaSinImportarMayusculasYVeAlPropietario() {
        HistorialVehiculoResponse res = service.historialPorPlaca("  def-456 ");

        assertEquals("DEF-456", res.getPlaca());
        assertEquals("Cliente Prueba", res.getPropietario().nombre());
        assertEquals("cliente2@mail.com", res.getPropietario().correo());
        assertEquals(1, res.getTotalMantenimientos());
        assertEquals("AS-2026-00035", res.getMantenimientos().get(0).getTicket());
    }

    @Test
    void lanzaNotFoundSiLaPlacaNoExiste() {
        assertThrows(NotFoundException.class, () -> service.historialPorPlaca("ZZZ-999"));
    }

    @Test
    void elClienteSigueSinPoderVerUnVehiculoAjeno() {
        assertThrows(ForbiddenException.class, () -> service.historial("otro@mail.com", 7L));
    }
}