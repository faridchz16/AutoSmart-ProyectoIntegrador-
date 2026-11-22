package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.dto.DiaTallerDto;
import com.tecsup.autosmart.service.ConfiguracionTallerService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HorarioManejador implements ManejadorIntencion {

    private static final String[] CODIGOS = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"};
    private static final String[] NOMBRES = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private final ConfiguracionTallerService tallerService;

    public HorarioManejador(ConfiguracionTallerService tallerService) {
        this.tallerService = tallerService;
    }

    @Override
    public String intencion() {
        return "horario";
    }

    @Override
    public int prioridad() {
        return 20;
    }

    @Override
    public boolean aplica(MensajeChat mensaje) {
        return TextoChat.contieneAlguno(mensaje, "horario", "horarios", "hora", "abren", "abre", "cierran",
                "cierra", "abierto", "atienden", "atencion", "horario de atencion");
    }

    @Override
    public RespuestaChat responder(MensajeChat mensaje) {
        List<DiaTallerDto> dias = tallerService.listarHorario();

        List<Map<String, Object>> datos = dias.stream()
                .filter(d -> Boolean.TRUE.equals(d.getAbierto()))
                .map(d -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("dia", CODIGOS[d.getDiaSemana() - 1]);
                    item.put("abre", d.getHoraApertura());
                    item.put("cierra", d.getHoraCierre());
                    return item;
                }).toList();

        String texto = dias.stream()
                .map(d -> NOMBRES[d.getDiaSemana() - 1] + ": " + (Boolean.TRUE.equals(d.getAbierto())
                        ? d.getHoraApertura() + " a " + d.getHoraCierre()
                        : "cerrado"))
                .collect(Collectors.joining("\n"));

        return RespuestaChat.de(intencion(), "Nuestro horario de atención es:\n" + texto,
                List.of(OpcionesChat.VER_SERVICIOS, OpcionesChat.PREGUNTAS_FRECUENTES),
                Map.of("dias", datos));
    }
}