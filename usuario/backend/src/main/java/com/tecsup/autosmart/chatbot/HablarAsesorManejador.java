package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.model.Derivacion;
import com.tecsup.autosmart.model.EmisorChat;
import com.tecsup.autosmart.model.MensajeChatbot;
import com.tecsup.autosmart.service.DerivacionService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HablarAsesorManejador implements ManejadorIntencion {

    public static final String INTENCION = "hablar_asesor";

    private static final Pattern CORREO = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern TELEFONO = Pattern.compile("(\\+?51[\\s-]?)?9\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{3}");

    private final DerivacionService derivacionService;

    public HablarAsesorManejador(DerivacionService derivacionService) {
        this.derivacionService = derivacionService;
    }

    @Override
    public String intencion() {
        return INTENCION;
    }

    @Override
    public int prioridad() {
        return 4;
    }

    @Override
    public boolean aplica(MensajeChat mensaje) {
        return pideAsesor(mensaje) || (contacto(mensaje.original()) != null && esperaContacto(mensaje));
    }

    @Override
    public RespuestaChat responder(MensajeChat mensaje) {
        Optional<Derivacion> pendiente = derivacionService.pendienteDe(mensaje.idConversacion());
        if (pendiente.isPresent()) {
            return confirmacion("Ya registramos tu solicitud (N.° " + pendiente.get().getIdDerivacion()
                    + "). Un asesor te contactará pronto.", pendiente.get());
        }

        String contacto = contacto(mensaje.original());
        if (!mensaje.conSesion() && contacto == null) {
            return RespuestaChat.de(intencion(),
                    "Con gusto te comunico con un asesor. Escríbeme tu teléfono (9 dígitos) o tu correo para que te contacte.",
                    List.of(), null);
        }

        Derivacion derivacion = derivacionService.registrar(
                mensaje.idConversacion(), mensaje.usuario(), contacto, mensaje.original());
        return confirmacion("¡Listo! Registramos tu solicitud (N.° " + derivacion.getIdDerivacion()
                + "). Un asesor te contactará en horario de atención.", derivacion);
    }

    private RespuestaChat confirmacion(String texto, Derivacion derivacion) {
        return RespuestaChat.de(intencion(), texto,
                List.of(OpcionesChat.VER_SERVICIOS, OpcionesChat.HORARIO),
                Map.of("idDerivacion", derivacion.getIdDerivacion()));
    }

    private boolean pideAsesor(MensajeChat mensaje) {
        return TextoChat.contieneAlguno(mensaje, "asesor", "asesora", "hablar con una persona", "hablar con alguien",
                "humano", "persona real", "agente", "operador", "hablar con un asesor", "atencion personalizada");
    }

    private boolean esperaContacto(MensajeChat mensaje) {
        List<MensajeChatbot> ultimos = derivacionService.ultimosMensajes(mensaje.idConversacion());
        if (ultimos.isEmpty()) {
            return false;
        }
        MensajeChatbot ultimo = ultimos.get(ultimos.size() - 1);
        return ultimo.getEmisor() == EmisorChat.BOT && INTENCION.equals(ultimo.getIntencion())
                && derivacionService.pendienteDe(mensaje.idConversacion()).isEmpty();
    }

    static String contacto(String texto) {
        if (texto == null) {
            return null;
        }
        Matcher correo = CORREO.matcher(texto);
        if (correo.find()) {
            return correo.group().toLowerCase();
        }
        Matcher telefono = TELEFONO.matcher(texto);
        if (telefono.find()) {
            String digitos = telefono.group().replaceAll("\\D", "");
            return digitos.substring(digitos.length() - 9);
        }
        return null;
    }
}