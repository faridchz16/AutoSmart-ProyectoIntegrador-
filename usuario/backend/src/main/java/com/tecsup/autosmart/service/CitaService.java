package com.tecsup.autosmart.service;

import com.tecsup.autosmart.dto.CitaCreadaResponse;
import com.tecsup.autosmart.dto.CitaDetalleResponse;
import com.tecsup.autosmart.dto.CitaRequest;
import com.tecsup.autosmart.exception.ConflictException;
import com.tecsup.autosmart.exception.ForbiddenException;
import com.tecsup.autosmart.exception.NotFoundException;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.model.Servicio;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.model.Vehiculo;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.repository.ServicioRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import com.tecsup.autosmart.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;

@Service
public class CitaService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final VehiculoRepository vehiculoRepository;
    private final TurnoService turnoService;

    public CitaService(CitaRepository citaRepository,
                       UsuarioRepository usuarioRepository,
                       ServicioRepository servicioRepository,
                       VehiculoRepository vehiculoRepository,
                       TurnoService turnoService) {
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioRepository = servicioRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.turnoService = turnoService;
    }

    @Transactional
    public CitaCreadaResponse reservar(String correoCliente, CitaRequest req) {

        Usuario mecanico = usuarioRepository.buscarParaReserva(req.getIdMecanico())
                .orElseThrow(() -> new NotFoundException("Mecanico no encontrado"));

        Usuario cliente = usuarioRepository.findByCorreo(correoCliente)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Servicio servicio = servicioRepository.findByIdServicioAndActivoTrue(req.getIdServicio())
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));

        Vehiculo vehiculo = vehiculoRepository.findById(req.getIdVehiculo())
                .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                .orElseThrow(() -> new NotFoundException("Vehiculo no encontrado"));
        if (!vehiculo.getCliente().getIdUsuario().equals(cliente.getIdUsuario())) {
            throw new ForbiddenException("El vehiculo no te pertenece");
        }

        boolean libre = turnoService.turnosLibres(req.getIdServicio(), req.getIdMecanico(), req.getFecha())
                .stream()
                .anyMatch(t -> t.getHoraInicio().equals(req.getHoraInicio()));
        if (!libre) {
            throw new ConflictException("El turno ya no esta disponible");
        }

        LocalTime horaFin = req.getHoraInicio().plusMinutes(servicio.getDuracionMinutos());

        Cita cita = new Cita();
        cita.setTicket("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        cita.setCliente(cliente);
        cita.setMecanico(mecanico);
        cita.setVehiculo(vehiculo);
        cita.setServicio(servicio);
        cita.setFecha(req.getFecha());
        cita.setHoraInicio(req.getHoraInicio());
        cita.setHoraFin(horaFin);
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setPrecio(servicio.getPrecioBase());
        cita = citaRepository.saveAndFlush(cita);

        // 4) Ticket AS-AAAA-NNNNN a partir del id (nunca se repite)
        int anio = LocalDate.now(LIMA).getYear();
        cita.setTicket(String.format("AS-%d-%05d", anio, cita.getIdCita()));
        cita = citaRepository.save(cita);

        CitaCreadaResponse res = new CitaCreadaResponse();
        res.setIdCita(cita.getIdCita());
        res.setTicket(cita.getTicket());
        res.setEstado(cita.getEstado().name());
        res.setFecha(cita.getFecha().toString());
        res.setHoraInicio(cita.getHoraInicio().toString());
        res.setHoraFin(cita.getHoraFin().toString());
        res.setPrecio(cita.getPrecio());
        res.setServicio(servicio.getNombre());
        res.setMecanico(mecanico.getNombre());
        res.setPlaca(vehiculo.getPlaca());
        return res;
    }


    @Transactional
    public CitaCreadaResponse aceptar(String correoMecanico, Long idCita) {
        Cita cita = citaPendienteDelMecanico(correoMecanico, idCita);
        cita.setEstado(EstadoCita.CONFIRMADA);
        return aRespuesta(citaRepository.save(cita));
    }


    @Transactional
    public CitaCreadaResponse rechazar(String correoMecanico, Long idCita, String motivo) {
        Cita cita = citaPendienteDelMecanico(correoMecanico, idCita);
        cita.setEstado(EstadoCita.RECHAZADA);
        cita.setMotivoRechazo(motivo.trim());
        return aRespuesta(citaRepository.save(cita));
    }

    private Cita citaPendienteDelMecanico(String correoMecanico, Long idCita) {
        Cita cita = citaRepository.buscarDetalle(idCita)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));

        if (!cita.getMecanico().getCorreo().equalsIgnoreCase(correoMecanico)) {
            throw new ForbiddenException("Solo el mecanico asignado puede responder esta cita");
        }
        if (cita.getEstado() != EstadoCita.PENDIENTE) {
            throw new ConflictException("La cita ya no esta pendiente (estado: " + cita.getEstado() + ")");
        }
        return cita;
    }

    @Transactional(readOnly = true)
    public List<CitaCreadaResponse> misCitas(String correoCliente, boolean futuras) {
        Usuario cliente = usuarioRepository.findByCorreo(correoCliente)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        List<Cita> citas = futuras
                ? citaRepository.buscarFuturasDelCliente(cliente, LocalDate.now(LIMA),
                List.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA))
                : citaRepository.buscarDelCliente(cliente);

        return citas.stream().map(this::aRespuesta).toList();
    }

    private CitaCreadaResponse aRespuesta(Cita c) {
        CitaCreadaResponse res = new CitaCreadaResponse();
        res.setIdCita(c.getIdCita());
        res.setTicket(c.getTicket());
        res.setEstado(c.getEstado().name());
        res.setFecha(c.getFecha().toString());
        res.setHoraInicio(c.getHoraInicio().toString());
        res.setHoraFin(c.getHoraFin().toString());
        res.setPrecio(c.getPrecio());
        res.setServicio(c.getServicio().getNombre());
        res.setMecanico(c.getMecanico().getNombre());
        res.setPlaca(c.getVehiculo().getPlaca());
        return res;
    }

    @Transactional(readOnly = true)
    public CitaDetalleResponse detalleParaMecanico(String correoMecanico, Long idCita) {
        Cita cita = citaRepository.buscarDetalle(idCita)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));

        if (!cita.getMecanico().getCorreo().equalsIgnoreCase(correoMecanico)) {
            throw new ForbiddenException("Solo el mecanico asignado puede ver esta cita");
        }
        return mapear(cita);
    }

    private CitaDetalleResponse mapear(Cita c) {
        CitaDetalleResponse res = new CitaDetalleResponse();
        res.setIdCita(c.getIdCita());
        res.setTicket(c.getTicket());
        res.setFecha(c.getFecha().toString());
        res.setHoraInicio(c.getHoraInicio().toString());
        res.setHoraFin(c.getHoraFin().toString());
        res.setEstado(c.getEstado().name());
        res.setPrecio(c.getPrecio());
        res.setServicio(c.getServicio().getNombre());
        res.setDuracionMinutos(c.getServicio().getDuracionMinutos());
        res.setCliente(c.getCliente().getNombre());
        res.setTelefonoCliente(c.getCliente().getTelefono());
        res.setPlaca(c.getVehiculo().getPlaca());
        res.setMarca(c.getVehiculo().getMarca());
        res.setModelo(c.getVehiculo().getModelo());
        res.setAnio(c.getVehiculo().getAnio());
        res.setKilometraje(c.getVehiculo().getKilometraje());
        return res;
    }
}