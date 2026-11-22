package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.dto.ServicioResponse;
import com.tecsup.autosmart.service.ServicioService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class CotizarManejador implements ManejadorIntencion {

    private static final BigDecimal MARGEN_ADICIONALES = new BigDecimal("1.20");
    private static final Set<String> CONECTORES = Set.of("de", "del", "la", "el", "los", "las", "y", "en", "para", "con");

    private final ServicioService servicioService;

    public CotizarManejador(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @Override
    public String intencion() {
        return "cotizar";
    }

    @Override
    public int prioridad() {
        return 8;
    }

    @Override
    public boolean aplica(MensajeChat mensaje) {
        return TextoChat.contieneAlguno(mensaje, "cuanto cuesta", "cuanto cuestan", "cuanto sale", "cuanto vale",
                "cuanto cobran", "cuesta", "precio", "precios", "costo", "cotizar", "cotizacion", "tarifa");
    }

    @Override
    public RespuestaChat responder(MensajeChat mensaje) {
        List<ServicioResponse> servicios = servicioService.listarActivos(null, null);
        Optional<ServicioResponse> encontrado = buscar(servicios, mensaje);

        if (encontrado.isEmpty()) {
            List<String> opciones = servicios.stream().limit(6).map(s -> "Precio de " + s.getNombre()).toList();
            return RespuestaChat.de(intencion(), "¿De qué servicio quieres saber el precio?",
                    opciones.isEmpty() ? List.of(OpcionesChat.VER_SERVICIOS) : opciones, null);
        }

        ServicioResponse servicio = encontrado.get();
        BigDecimal desde = redondear(servicio.getPrecioBase());
        BigDecimal hasta = redondear(desde.multiply(MARGEN_ADICIONALES));

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("servicio", servicio.getNombre());
        datos.put("precioDesde", desde);
        datos.put("precioHasta", hasta);
        datos.put("referencial", true);

        return RespuestaChat.de(intencion(),
                "El servicio de " + servicio.getNombre() + " cuesta desde S/ " + desde.toPlainString()
                        + ". Es un precio referencial: el costo final se confirma en la inspección.",
                List.of(OpcionesChat.VER_SERVICIOS, OpcionesChat.HORARIO), datos);
    }

    private Optional<ServicioResponse> buscar(List<ServicioResponse> servicios, MensajeChat mensaje) {
        return servicios.stream()
                .filter(s -> puntaje(s, mensaje) > 0)
                .max(Comparator.comparingInt(s -> puntaje(s, mensaje)));
    }

    private int puntaje(ServicioResponse servicio, MensajeChat mensaje) {
        String nombre = TextoChat.normalizar(servicio.getNombre());
        if (!nombre.isEmpty() && (" " + mensaje.normalizado() + " ").contains(" " + nombre + " ")) {
            return 100;
        }
        return (int) Arrays.stream(nombre.split(" "))
                .filter(p -> p.length() > 2 && !CONECTORES.contains(p))
                .filter(p -> mensaje.palabras().contains(p))
                .count();
    }

    private BigDecimal redondear(BigDecimal monto) {
        return (monto == null ? BigDecimal.ZERO : monto).setScale(2, RoundingMode.HALF_UP);
    }
}