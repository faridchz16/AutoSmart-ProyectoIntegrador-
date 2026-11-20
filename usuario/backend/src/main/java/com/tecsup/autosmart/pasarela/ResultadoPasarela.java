package com.tecsup.autosmart.pasarela;

public record ResultadoPasarela(boolean aprobado, String referencia, String motivoRechazo) {

    public static ResultadoPasarela aprobado(String referencia) {
        return new ResultadoPasarela(true, referencia, null);
    }

    public static ResultadoPasarela rechazado(String referencia, String motivo) {
        return new ResultadoPasarela(false, referencia, motivo);
    }
}