package com.tecsup.autosmart.chatbot;

import com.tecsup.autosmart.model.Usuario;

import java.util.Set;

public record MensajeChat(
        String idConversacion,
        String original,
        String normalizado,
        Set<String> palabras,
        Usuario usuario
) {

    public static MensajeChat de(String idConversacion, String texto, Usuario usuario) {
        String normalizado = TextoChat.normalizar(texto);
        return new MensajeChat(idConversacion, texto, normalizado, TextoChat.palabras(normalizado), usuario);
    }

    public boolean conSesion() {
        return usuario != null;
    }
}