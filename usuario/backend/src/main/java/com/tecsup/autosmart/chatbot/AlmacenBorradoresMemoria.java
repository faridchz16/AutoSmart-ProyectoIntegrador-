package com.tecsup.autosmart.chatbot;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlmacenBorradoresMemoria implements AlmacenBorradores {

    private static final Duration VIGENCIA = Duration.ofMinutes(30);

    private final Map<String, Entrada> borradores = new ConcurrentHashMap<>();
    private final Clock clock;

    public AlmacenBorradoresMemoria(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<BorradorCita> obtener(String idConversacion) {
        Instant ahora = clock.instant();
        borradores.values().removeIf(e -> e.expira().isBefore(ahora));
        return Optional.ofNullable(borradores.get(idConversacion)).map(Entrada::borrador);
    }

    @Override
    public void guardar(String idConversacion, BorradorCita borrador) {
        borradores.put(idConversacion, new Entrada(borrador, clock.instant().plus(VIGENCIA)));
    }

    @Override
    public void eliminar(String idConversacion) {
        borradores.remove(idConversacion);
    }

    private record Entrada(BorradorCita borrador, Instant expira) {
    }
}