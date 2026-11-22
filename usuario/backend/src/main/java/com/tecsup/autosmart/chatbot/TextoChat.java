package com.tecsup.autosmart.chatbot;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class TextoChat {

    private TextoChat() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9ñ\\- ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static Set<String> palabras(String textoNormalizado) {
        if (textoNormalizado == null || textoNormalizado.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(textoNormalizado.split(" "))
                .filter(p -> !p.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static boolean contieneTermino(MensajeChat mensaje, String termino) {
        String normalizado = normalizar(termino);
        if (normalizado.isEmpty()) {
            return false;
        }
        if (normalizado.contains(" ")) {
            return (" " + mensaje.normalizado() + " ").contains(" " + normalizado + " ");
        }
        return mensaje.palabras().contains(normalizado);
    }

    public static boolean contieneAlguno(MensajeChat mensaje, String... terminos) {
        return Arrays.stream(terminos).anyMatch(t -> contieneTermino(mensaje, t));
    }
}