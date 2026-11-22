package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.service.ServicioService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ServiciosManejador implements ManejadorIntencion {

    private final ServicioService servicioService;

    public ServiciosManejador(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @Override
    public String intencion() {
        return "servicios";
    }

    @Override
    public int prioridad() {
        return 30;
    }

    @Override
    public boolean aplica(MensajeChat mensaje) {
        return TextoChat.contieneAlguno(mensaje, "servicio", "servicios", "ver servicios", "que hacen", "catalogo");
    }

    @Override
    public RespuestaChat responder(MensajeChat mensaje) {
        List<ServicioResponse> servicios = servicioService.listarActivos(null, null);
        if (servicios.isEmpty()) {
            return RespuestaChat.de(intencion(), "Por el momento no tenemos servicios disponibles.",
                    List.of(OpcionesChat.HORARIO), null);
        }

        List<Map<String, Object>> datos = servicios.stream().map(s -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getIdServicio());
            item.put("nombre", s.getNombre());
            item.put("precio", s.getPrecioBase());
            return item;
        }).toList();

        String lista = servicios.stream()
                .map(s -> "• " + s.getNombre() + " (desde S/ " + soles(s.getPrecioBase()) + ")")
                .collect(Collectors.joining("\n"));

        return RespuestaChat.de(intencion(), "Estos son nuestros servicios:\n" + lista,
                List.of(OpcionesChat.HORARIO, OpcionesChat.PREGUNTAS_FRECUENTES),
                Map.of("servicios", datos));
    }

    private String soles(BigDecimal monto) {
        return (monto == null ? BigDecimal.ZERO : monto).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}