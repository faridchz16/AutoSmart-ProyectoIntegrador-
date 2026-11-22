package com.tecsup.autosmart.service;

import com.tecsup.autosmart.chatbot.ManejadorIntencion;
import com.tecsup.autosmart.chatbot.MensajeChat;
import com.tecsup.autosmart.chatbot.OpcionesChat;
import com.tecsup.autosmart.chatbot.RespuestaChat;
import com.tecsup.autosmart.dto.ChatbotRequest;
import com.tecsup.autosmart.dto.ChatbotResponse;
import com.tecsup.autosmart.model.EmisorChat;
import com.tecsup.autosmart.model.MensajeChatbot;
import com.tecsup.autosmart.model.Usuario;
import com.tecsup.autosmart.repository.MensajeChatbotRepository;
import com.tecsup.autosmart.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);
    private static final int MAX_TEXTO = 1000;

    private final List<ManejadorIntencion> manejadores;
    private final MensajeChatbotRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public ChatbotService(List<ManejadorIntencion> manejadores,
                          MensajeChatbotRepository mensajeRepository,
                          UsuarioRepository usuarioRepository,
                          Clock clock) {
        this.manejadores = manejadores.stream()
                .sorted(Comparator.comparingInt(ManejadorIntencion::prioridad))
                .toList();
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    public ChatbotResponse procesar(String correo, ChatbotRequest request) {
        String idConversacion = resolverConversacion(request.idConversacion());
        Usuario usuario = correo == null ? null : usuarioRepository.findByCorreo(correo).orElse(null);
        MensajeChat mensaje = MensajeChat.de(idConversacion, request.mensaje().trim(), usuario);

        RespuestaChat respuesta = responder(mensaje);

        guardar(idConversacion, usuario, EmisorChat.USUARIO, mensaje.original(), respuesta.intencion());
        guardar(idConversacion, usuario, EmisorChat.BOT, respuesta.respuesta(), respuesta.intencion());

        return new ChatbotResponse(idConversacion, respuesta.intencion(), respuesta.respuesta(),
                respuesta.opciones(), respuesta.datos(), respuesta.requiereLogin());
    }

    private RespuestaChat responder(MensajeChat mensaje) {
        for (ManejadorIntencion manejador : manejadores) {
            if (!manejador.aplica(mensaje)) {
                continue;
            }
            if (manejador.requiereSesion() && !mensaje.conSesion()) {
                return RespuestaChat.requiereSesion(manejador.intencion());
            }
            try {
                return manejador.responder(mensaje);
            } catch (RuntimeException e) {
                log.error("Error en la intencion {} del chatbot", manejador.intencion(), e);
                return RespuestaChat.de(manejador.intencion(),
                        "Tuve un problema al consultar esa información. Inténtalo de nuevo en unos minutos.",
                        List.of(OpcionesChat.VER_SERVICIOS, OpcionesChat.HORARIO), null);
            }
        }
        return noEntendido();
    }

    private RespuestaChat noEntendido() {
        return RespuestaChat.de("no_entendido",
                "No entendí tu mensaje. Puedo ayudarte con estas opciones:",
                List.of(OpcionesChat.VER_SERVICIOS, OpcionesChat.HORARIO, OpcionesChat.PREGUNTAS_FRECUENTES),
                null);
    }

    private String resolverConversacion(String idConversacion) {
        if (idConversacion != null) {
            try {
                return UUID.fromString(idConversacion.trim()).toString();
            } catch (IllegalArgumentException e) {
                log.debug("idConversacion invalido, se crea una nueva conversacion");
            }
        }
        return UUID.randomUUID().toString();
    }

    private void guardar(String idConversacion, Usuario usuario, EmisorChat emisor, String texto, String intencion) {
        String recortado = texto.length() > MAX_TEXTO ? texto.substring(0, MAX_TEXTO) : texto;
        mensajeRepository.save(new MensajeChatbot(idConversacion, usuario, emisor, recortado, intencion,
                LocalDateTime.now(clock).withNano(0)));
    }
}