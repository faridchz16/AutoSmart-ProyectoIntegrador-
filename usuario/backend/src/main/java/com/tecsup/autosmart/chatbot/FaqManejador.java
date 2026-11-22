package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.model.Faq;
import com.tecsup.autosmart.service.FaqService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class FaqManejador implements ManejadorIntencion {

    private final FaqService faqService;

    public FaqManejador(FaqService faqService) {
        this.faqService = faqService;
    }

    @Override
    public String intencion() {
        return "faq";
    }

    @Override
    public int prioridad() {
        return 10;
    }

    @Override
    public boolean aplica(MensajeChat mensaje) {
        return pideListado(mensaje) || buscar(mensaje).isPresent();
    }

    @Override
    public RespuestaChat responder(MensajeChat mensaje) {
        if (pideListado(mensaje)) {
            List<String> preguntas = faqService.activas().stream().map(Faq::getPregunta).limit(6).toList();
            if (preguntas.isEmpty()) {
                return RespuestaChat.de(intencion(), "Por ahora no tenemos preguntas frecuentes registradas.",
                        List.of(OpcionesChat.VER_SERVICIOS, OpcionesChat.HORARIO), null);
            }
            return RespuestaChat.de(intencion(), "Estas son las preguntas más frecuentes. Toca una para ver la respuesta.",
                    preguntas, null);
        }
        Faq faq = buscar(mensaje).orElseThrow();
        return RespuestaChat.de(intencion(), faq.getRespuesta(),
                List.of(OpcionesChat.PREGUNTAS_FRECUENTES, OpcionesChat.VER_SERVICIOS),
                Map.of("idFaq", faq.getIdFaq()));
    }

    private boolean pideListado(MensajeChat mensaje) {
        return TextoChat.contieneAlguno(mensaje, "preguntas frecuentes", "faq", "dudas frecuentes");
    }

    private Optional<Faq> buscar(MensajeChat mensaje) {
        return faqService.activas().stream()
                .filter(f -> puntaje(f, mensaje) > 0)
                .max(Comparator.comparingInt(f -> puntaje(f, mensaje)));
    }

    private int puntaje(Faq faq, MensajeChat mensaje) {
        if (TextoChat.normalizar(faq.getPregunta()).equals(mensaje.normalizado())) {
            return 100;
        }
        if (faq.getPalabrasClave() == null) {
            return 0;
        }
        return (int) Arrays.stream(faq.getPalabrasClave().split(","))
                .filter(p -> TextoChat.contieneTermino(mensaje, p))
                .count();
    }
}