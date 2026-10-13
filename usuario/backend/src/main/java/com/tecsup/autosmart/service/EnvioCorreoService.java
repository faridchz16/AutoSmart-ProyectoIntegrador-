package com.tecsup.autosmart.service;

public interface EnvioCorreoService {
    void enviarCodigoRecuperacion(String correo, String nombre, String codigo, int minutosVigencia);
}