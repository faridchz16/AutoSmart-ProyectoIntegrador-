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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AgendarManejador implements ManejadorIntencion {

    private static final Logger log = LoggerFactory.getLogger(AgendarManejador.class);
    private static final Locale ES = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final Pattern FECHA_ISO = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern FECHA_CORTA = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{4}))?");
    private static final Pattern HORA = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?");
    private static final int DIAS_A_BUSCAR = 14;
    private static final int MAX_OPCIONES = 8;
    private static final String CONFIRMAR = "Confirmar";
    private static final String CANCELAR = "Cancelar";

    private final AlmacenBorradores almacen;
    private final ServicioService servicioService;
    private final VehiculoRepository vehiculoRepository;
    private final DisponibilidadService disponibilidadService;
    private final TurnoService turnoService;
    private final CitaService citaService;
    private final Clock clock;

    public AgendarManejador(AlmacenBorradores almacen,
                            ServicioService servicioService,
                            VehiculoRepository vehiculoRepository,
                            DisponibilidadService disponibilidadService,
                            TurnoService turnoService,
                            CitaService citaService,
                            Clock clock) {
        this.almacen = almacen;
        this.servicioService = servicioService;
        this.vehiculoRepository = vehiculoRepository;
        this.disponibilidadService = disponibilidadService;
        this.turnoService = turnoService;
        this.citaService = citaService;
        this.clock = clock;
    }

    @Override
    public String intencion() {
        return "agendar";
    }

    @Override
    public int prioridad() {
        return 3;
    }

    @Override
    public boolean requiereSesion() {
        return true;
    }

    @Override
    public boolean aplica(MensajeChat mensaje) {
        return almacen.obtener(mensaje.idConversacion()).isPresent()
                || TextoChat.contieneAlguno(mensaje, "agendar", "agenda", "reservar", "reserva", "quiero una cita",
                "nueva cita", "sacar una cita", "sacar cita", "separar una cita", "programar una cita",
                "agendar una cita");
    }

    @Override
    public RespuestaChat responder(MensajeChat mensaje) {
        String conversacion = mensaje.idConversacion();
        Optional<BorradorCita> existente = almacen.obtener(conversacion);

        if (existente.isPresent() && TextoChat.contieneAlguno(mensaje, "cancelar", "cancela", "salir")) {
            almacen.eliminar(conversacion);
            return RespuestaChat.de(intencion(), "Listo, cancelé el agendamiento. No se creó ninguna cita.",
                    List.of(OpcionesChat.VER_SERVICIOS, OpcionesChat.HORARIO), null);
        }

        if (existente.isEmpty()) {
            BorradorCita nuevo = new BorradorCita();
            almacen.guardar(conversacion, nuevo);
            Optional<ServicioResponse> servicio = buscarServicio(mensaje);
            if (servicio.isEmpty()) {
                return preguntarServicio("¡Perfecto! Vamos a agendar tu cita. ¿Qué servicio necesitas?");
            }
            nuevo.elegirServicio(servicio.get().getIdServicio(), servicio.get().getNombre(), servicio.get().getPrecioBase());
            return pasarAVehiculo(nuevo, mensaje, "Agendaremos " + servicio.get().getNombre() + ". ");
        }

        BorradorCita borrador = existente.get();
        RespuestaChat respuesta = switch (borrador.getPaso()) {
            case SERVICIO -> responderServicio(borrador, mensaje);
            case VEHICULO -> responderVehiculo(borrador, mensaje);
            case FECHA -> responderFecha(borrador, mensaje);
            case TURNO -> responderTurno(borrador, mensaje);
            case CONFIRMAR -> responderConfirmacion(borrador, mensaje);
        };
        if (almacen.obtener(conversacion).isPresent()) {
            almacen.guardar(conversacion, borrador);
        }
        return respuesta;
    }

    private RespuestaChat responderServicio(BorradorCita borrador, MensajeChat mensaje) {
        Optional<ServicioResponse> servicio = buscarServicio(mensaje);
        if (servicio.isEmpty()) {
            return preguntarServicio("No reconocí el servicio. Elige una de las opciones o escribe Cancelar.");
        }
        borrador.elegirServicio(servicio.get().getIdServicio(), servicio.get().getNombre(), servicio.get().getPrecioBase());
        return pasarAVehiculo(borrador, mensaje, "");
    }

    private RespuestaChat pasarAVehiculo(BorradorCita borrador, MensajeChat mensaje, String prefijo) {
        List<Vehiculo> vehiculos = vehiculoRepository.findByClienteAndActivoTrue(mensaje.usuario());
        if (vehiculos.isEmpty()) {
            almacen.eliminar(mensaje.idConversacion());
            return RespuestaChat.de(intencion(),
                    "No tienes vehículos registrados. Registra tu vehículo en la app y vuelve a intentarlo.",
                    List.of(OpcionesChat.VER_SERVICIOS), null);
        }
        if (vehiculos.size() == 1) {
            Vehiculo unico = vehiculos.get(0);
            borrador.elegirVehiculo(unico.getIdVehiculo(), unico.getPlaca());
            return pasarAFecha(borrador, mensaje, prefijo + "Usaré tu vehículo " + unico.getPlaca() + ". ");
        }
        borrador.setPaso(PasoAgendar.VEHICULO);
        return RespuestaChat.de(intencion(), prefijo + "¿Para cuál de tus vehículos es la cita?",
                conCancelar(vehiculos.stream().map(Vehiculo::getPlaca).toList()), null);
    }

    private RespuestaChat responderVehiculo(BorradorCita borrador, MensajeChat mensaje) {
        String texto = compactar(mensaje.original());
        List<Vehiculo> vehiculos = vehiculoRepository.findByClienteAndActivoTrue(mensaje.usuario());
        Optional<Vehiculo> elegido = vehiculos.stream()
                .filter(v -> !compactar(v.getPlaca()).isEmpty() && texto.contains(compactar(v.getPlaca())))
                .findFirst();
        if (elegido.isEmpty()) {
            return RespuestaChat.de(intencion(), "No reconocí la placa. Elige uno de tus vehículos o escribe Cancelar.",
                    conCancelar(vehiculos.stream().map(Vehiculo::getPlaca).toList()), null);
        }
        borrador.elegirVehiculo(elegido.get().getIdVehiculo(), elegido.get().getPlaca());
        return pasarAFecha(borrador, mensaje, "");
    }

    private RespuestaChat pasarAFecha(BorradorCita borrador, MensajeChat mensaje, String prefijo) {
        borrador.setPaso(PasoAgendar.FECHA);
        List<String> fechas = fechasDisponibles(borrador, mensaje.usuario());
        if (fechas.isEmpty()) {
            almacen.eliminar(mensaje.idConversacion());
            return RespuestaChat.de(intencion(),
                    prefijo + "Lo siento, no hay turnos libres para " + borrador.getServicio()
                            + " en los próximos días. Intenta más tarde o habla con un asesor.",
                    List.of(OpcionesChat.VER_SERVICIOS), null);
        }
        return RespuestaChat.de(intencion(), prefijo + "¿Qué día prefieres? Estas son las fechas con turnos libres:",
                conCancelar(fechas), null);
    }

    private RespuestaChat responderFecha(BorradorCita borrador, MensajeChat mensaje) {
        LocalDate fecha = leerFecha(mensaje);
        LocalDate hoy = LocalDate.now(clock);
        if (fecha == null || fecha.isBefore(hoy)) {
            return RespuestaChat.de(intencion(), "No entendí la fecha. Elige una opción o escríbela como 25/11.",
                    conCancelar(fechasDisponibles(borrador, mensaje.usuario())), null);
        }
        Map<LocalTime, MecanicoDisponibleResponse> turnos = turnosDelDia(borrador, fecha, mensaje.usuario());
        if (turnos.isEmpty()) {
            return RespuestaChat.de(intencion(), "No hay turnos libres el " + fecha.format(DIA_MES) + ". Elige otra fecha:",
                    conCancelar(fechasDisponibles(borrador, mensaje.usuario())), null);
        }
        borrador.setFecha(fecha);
        borrador.setPaso(PasoAgendar.TURNO);
        return preguntarTurno("¿A qué hora? Turnos libres el " + fecha.format(DIA_MES) + ":", turnos);
    }

    private RespuestaChat responderTurno(BorradorCita borrador, MensajeChat mensaje) {
        Map<LocalTime, MecanicoDisponibleResponse> turnos = turnosDelDia(borrador, borrador.getFecha(), mensaje.usuario());
        LocalTime hora = leerHora(mensaje.original());
        MecanicoDisponibleResponse mecanico = hora == null ? null : turnos.get(hora);
        if (mecanico == null) {
            if (turnos.isEmpty()) {
                borrador.setPaso(PasoAgendar.FECHA);
                return RespuestaChat.de(intencion(), "Ya no quedan turnos ese día. Elige otra fecha:",
                        conCancelar(fechasDisponibles(borrador, mensaje.usuario())), null);
            }
            return preguntarTurno("Ese horario no está disponible. Elige uno de estos:", turnos);
        }
        borrador.elegirTurno(hora, mecanico.getIdMecanico(), mecanico.getNombre());
        borrador.setPaso(PasoAgendar.CONFIRMAR);
        return resumen(borrador);
    }

    private RespuestaChat responderConfirmacion(BorradorCita borrador, MensajeChat mensaje) {
        if (!TextoChat.contieneAlguno(mensaje, "confirmar", "confirmo", "si", "ok", "dale")) {
            return resumen(borrador);
        }

        CitaRequest request = new CitaRequest();
        request.setIdServicio(borrador.getIdServicio());
        request.setIdMecanico(borrador.getIdMecanico());
        request.setIdVehiculo(borrador.getIdVehiculo());
        request.setFecha(borrador.getFecha());
        request.setHoraInicio(borrador.getHoraInicio());

        try {
            CitaCreadaResponse cita = citaService.reservar(mensaje.usuario().getCorreo(), request);
            almacen.eliminar(mensaje.idConversacion());
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("idCita", cita.getIdCita());
            datos.put("ticket", cita.getTicket());
            datos.put("estado", cita.getEstado());
            return RespuestaChat.de(intencion(), "¡Listo! Tu cita " + cita.getTicket() + " quedó registrada para el "
                            + borrador.getFecha().format(DIA_MES) + " a las " + borrador.getHoraInicio()
                            + ". El mecánico la confirmará pronto.",
                    List.of("Ver mis citas"), datos);
        } catch (ConflictException e) {
            borrador.limpiarTurno();
            borrador.setPaso(PasoAgendar.TURNO);
            Map<LocalTime, MecanicoDisponibleResponse> turnos = turnosDelDia(borrador, borrador.getFecha(), mensaje.usuario());
            if (turnos.isEmpty()) {
                borrador.setPaso(PasoAgendar.FECHA);
                return RespuestaChat.de(intencion(), "Ese turno se acaba de ocupar y ya no quedan otros ese día. Elige otra fecha:",
                        conCancelar(fechasDisponibles(borrador, mensaje.usuario())), null);
            }
            return preguntarTurno("Ese turno se acaba de ocupar. Elige otro horario:", turnos);
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar la cita desde el chatbot: {}", e.getMessage());
            almacen.eliminar(mensaje.idConversacion());
            return RespuestaChat.de(intencion(), "No pude registrar la cita: " + e.getMessage()
                    + ". Puedes intentarlo de nuevo o agendarla desde la app.", List.of(OpcionesChat.VER_SERVICIOS), null);
        }
    }

    private RespuestaChat resumen(BorradorCita borrador) {
        String precio = borrador.getPrecio() == null ? ""
                : " Precio referencial: S/ " + borrador.getPrecio().setScale(2, RoundingMode.HALF_UP).toPlainString() + ".";
        String texto = "Revisa tu cita: " + borrador.getServicio() + " para el vehículo " + borrador.getPlaca()
                + ", el " + borrador.getFecha().format(DIA_MES) + " a las " + borrador.getHoraInicio()
                + " con " + borrador.getMecanico() + "." + precio + " ¿La confirmo?";
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("servicio", borrador.getServicio());
        datos.put("placa", borrador.getPlaca());
        datos.put("fecha", borrador.getFecha().toString());
        datos.put("hora", borrador.getHoraInicio().toString());
        datos.put("mecanico", borrador.getMecanico());
        return RespuestaChat.de(intencion(), texto, List.of(CONFIRMAR, CANCELAR), datos);
    }

    private RespuestaChat preguntarServicio(String texto) {
        List<String> nombres = servicioService.listarActivos(null, null).stream()
                .map(ServicioResponse::getNombre).limit(MAX_OPCIONES).toList();
        return RespuestaChat.de(intencion(), texto, conCancelar(nombres), null);
    }

    private RespuestaChat preguntarTurno(String texto, Map<LocalTime, MecanicoDisponibleResponse> turnos) {
        List<String> horas = turnos.keySet().stream().limit(MAX_OPCIONES).map(LocalTime::toString).toList();
        return RespuestaChat.de(intencion(), texto, conCancelar(horas), null);
    }

    private List<String> fechasDisponibles(BorradorCita borrador, Usuario cliente) {
        LocalDate hoy = LocalDate.now(clock);
        List<String> fechas = new ArrayList<>();
        for (int i = 0; i < DIAS_A_BUSCAR && fechas.size() < 5; i++) {
            LocalDate dia = hoy.plusDays(i);
            if (!turnosDelDia(borrador, dia, cliente).isEmpty()) {
                String nombre = dia.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
                fechas.add(Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + dia.format(DIA_MES));
            }
        }
        return fechas;
    }

    private Map<LocalTime, MecanicoDisponibleResponse> turnosDelDia(BorradorCita borrador, LocalDate fecha, Usuario cliente) {
        Map<LocalTime, MecanicoDisponibleResponse> turnos = new TreeMap<>();
        List<MecanicoDisponibleResponse> mecanicos = disponibilidadService.mecanicosPorServicio(borrador.getIdServicio());
        for (MecanicoDisponibleResponse mecanico : mecanicos) {
            try {
                for (TurnoResponse turno : turnoService.turnosLibres(borrador.getIdServicio(), mecanico.getIdMecanico(),
                        fecha, cliente.getIdUsuario())) {
                    turnos.putIfAbsent(turno.getHoraInicio(), mecanico);
                }
            } catch (RuntimeException e) {
                log.debug("Sin turnos para el mecanico {}: {}", mecanico.getIdMecanico(), e.getMessage());
            }
        }
        return turnos;
    }

    private Optional<ServicioResponse> buscarServicio(MensajeChat mensaje) {
        return servicioService.listarActivos(null, null).stream()
                .filter(s -> puntaje(s, mensaje) > 0)
                .max(Comparator.comparingInt(s -> puntaje(s, mensaje)));
    }

    private int puntaje(ServicioResponse servicio, MensajeChat mensaje) {
        String nombre = TextoChat.normalizar(servicio.getNombre());
        if (!nombre.isEmpty() && (" " + mensaje.normalizado() + " ").contains(" " + nombre + " ")) {
            return 100;
        }
        return (int) Arrays.stream(nombre.split(" "))
                .filter(p -> p.length() > 3)
                .filter(p -> mensaje.palabras().contains(p))
                .count();
    }

    private LocalDate leerFecha(MensajeChat mensaje) {
        LocalDate hoy = LocalDate.now(clock);
        if (TextoChat.contieneTermino(mensaje, "pasado manana")) {
            return hoy.plusDays(2);
        }
        if (TextoChat.contieneTermino(mensaje, "manana")) {
            return hoy.plusDays(1);
        }
        if (TextoChat.contieneTermino(mensaje, "hoy")) {
            return hoy;
        }
        try {
            Matcher iso = FECHA_ISO.matcher(mensaje.original());
            if (iso.find()) {
                return LocalDate.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)),
                        Integer.parseInt(iso.group(3)));
            }
            Matcher corta = FECHA_CORTA.matcher(mensaje.original());
            if (corta.find()) {
                int dia = Integer.parseInt(corta.group(1));
                int mes = Integer.parseInt(corta.group(2));
                int anio = corta.group(3) != null ? Integer.parseInt(corta.group(3)) : hoy.getYear();
                LocalDate fecha = LocalDate.of(anio, mes, dia);
                return corta.group(3) == null && fecha.isBefore(hoy) ? fecha.plusYears(1) : fecha;
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }

    static LocalTime leerHora(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.matches("\\d{1,2}")) {
            int sola = Integer.parseInt(limpio);
            return sola <= 23 ? LocalTime.of(sola, 0) : null;
        }
        Matcher m = HORA.matcher(texto.toLowerCase(Locale.ROOT));
        while (m.find()) {
            int hora = Integer.parseInt(m.group(1));
            int minutos = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
            String sufijo = m.group(3);
            if (m.group(2) == null && sufijo == null) {
                continue;
            }
            if (sufijo != null && sufijo.startsWith("p") && hora < 12) {
                hora += 12;
            }
            if (sufijo != null && sufijo.startsWith("a") && hora == 12) {
                hora = 0;
            }
            if (hora <= 23 && minutos <= 59) {
                return LocalTime.of(hora, minutos);
            }
        }
        return null;
    }

    private List<String> conCancelar(List<String> opciones) {
        List<String> resultado = new ArrayList<>(opciones);
        resultado.add(CANCELAR);
        return resultado;
    }

    private String compactar(String texto) {
        return texto == null ? "" : texto.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}