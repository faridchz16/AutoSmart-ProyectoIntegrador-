package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.dto.SeguimientoResponse;
import com.tecsup.autosmart.model.Cita;
import com.tecsup.autosmart.model.EstadoCita;
import com.tecsup.autosmart.repository.CitaRepository;
import com.tecsup.autosmart.service.SeguimientoService;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EstadoVehiculoManejador implements ManejadorIntencion {

    private static final Pattern PLACA = Pattern.compile("\\b([a-z0-9]{3})-?([a-z0-9]{3})\\b");
    private static final Set<EstadoCita> EN_CURSO = Set.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA,
            EstadoCita.EN_PROCESO, EstadoCita.ESPERA_APROBACION, EstadoCita.FINALIZADA);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM");

    private final CitaRepository citaRepository;
    private final SeguimientoService seguimientoService;

    public EstadoVehiculoManejador(CitaRepository citaRepository, SeguimientoService seguimientoService) {
        this.citaRepository = citaRepository;
        this.seguimientoService = seguimientoService;
    }

    @Override
    public String intencion() {
        return "consultar_estado";
    }

    @Override
    public int prioridad() {
        return 6;
    }

    @Override
    public boolean requiereSesion() {
        return true;
    }

    @Override
    public boolean aplica(MensajeChat mensaje) {
        return TextoChat.contieneAlguno(mensaje, "estado", "como va", "mi auto", "mi carro", "mi vehiculo",
                "avance", "seguimiento", "esta listo", "mi cita", "mis citas")
                || placaMencionada(mensaje) != null;
    }

    @Override
    public RespuestaChat responder(MensajeChat mensaje) {
        List<Cita> enCurso = citaRepository.buscarDelCliente(mensaje.usuario()).stream()
                .filter(c -> EN_CURSO.contains(c.getEstado()))
                .sorted(Comparator.comparing(Cita::getFecha).reversed())
                .toList();

        if (enCurso.isEmpty()) {
            return RespuestaChat.de(intencion(), "No tienes servicios en curso en este momento.",
                    List.of(OpcionesChat.VER_SERVICIOS, OpcionesChat.HORARIO), null);
        }

        String placa = placaMencionada(mensaje);
        List<Cita> candidatas = placa == null ? enCurso : enCurso.stream()
                .filter(c -> c.getVehiculo() != null && limpiar(c.getVehiculo().getPlaca()).equals(placa))
                .toList();

        if (candidatas.isEmpty()) {
            return RespuestaChat.de(intencion(), "No encontré un servicio en curso para esa placa entre tus vehículos.",
                    opcionesDePlacas(enCurso), null);
        }

        Cita cita = candidatas.get(0);
        SeguimientoResponse seguimiento = seguimientoService.seguimiento(
                mensaje.usuario().getCorreo(), cita.getIdCita());

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("ticket", seguimiento.getTicket());
        datos.put("placa", seguimiento.getPlaca());
        datos.put("estado", seguimiento.getEstado());
        datos.put("avance", seguimiento.getPorcentajeAvance());

        List<String> opciones = enCurso.size() > 1 && placa == null
                ? opcionesDePlacas(enCurso.subList(1, enCurso.size()))
                : List.of(OpcionesChat.VER_SERVICIOS);

        return RespuestaChat.de(intencion(), describir(cita, seguimiento), opciones, datos);
    }

    private String describir(Cita cita, SeguimientoResponse seguimiento) {
        String inicio = "Tu vehículo " + seguimiento.getPlaca() + " (ticket " + seguimiento.getTicket() + ") ";
        return inicio + switch (cita.getEstado()) {
            case PENDIENTE -> "tiene la cita pendiente de confirmación por el mecánico.";
            case CONFIRMADA -> "tiene la cita confirmada para el " + cita.getFecha().format(FECHA)
                    + " a las " + cita.getHoraInicio() + ".";
            case EN_PROCESO -> "está en reparación. Avance: " + seguimiento.getPorcentajeAvance() + " %.";
            case ESPERA_APROBACION -> "espera que apruebes un presupuesto adicional.";
            case FINALIZADA -> "ya está listo. Puedes pagar el servicio y recogerlo.";
            default -> "está en estado " + cita.getEstado().name() + ".";
        };
    }

    private List<String> opcionesDePlacas(List<Cita> citas) {
        return citas.stream()
                .filter(c -> c.getVehiculo() != null)
                .map(c -> "Estado de " + c.getVehiculo().getPlaca())
                .distinct()
                .limit(4)
                .toList();
    }

    private String placaMencionada(MensajeChat mensaje) {
        Matcher m = PLACA.matcher(mensaje.normalizado());
        while (m.find()) {
            String candidata = m.group(1) + m.group(2);
            if (candidata.chars().anyMatch(Character::isDigit) && candidata.chars().anyMatch(Character::isLetter)) {
                return candidata;
            }
        }
        return null;
    }

    private String limpiar(String placa) {
        return placa == null ? "" : placa.toLowerCase().replace("-", "").trim();
    }
}