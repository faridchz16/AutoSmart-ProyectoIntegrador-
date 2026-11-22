package com.tecsup.autosmart.chatbot;

import java.util.Optional;

public interface AlmacenBorradores {

    Optional<BorradorCita> obtener(String idConversacion);

    void guardar(String idConversacion, BorradorCita borrador);

    void eliminar(String idConversacion);
}