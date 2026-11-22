package com.tecsup.autosmart.chatbot;

public interface ManejadorIntencion {

    String intencion();

    int prioridad();

    boolean aplica(MensajeChat mensaje);

    RespuestaChat responder(MensajeChat mensaje);

    default boolean requiereSesion() {
        return false;
    }
}