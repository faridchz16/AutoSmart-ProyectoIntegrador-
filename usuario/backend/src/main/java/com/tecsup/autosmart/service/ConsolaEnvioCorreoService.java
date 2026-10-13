package com.tecsup.autosmart.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsolaEnvioCorreoService implements EnvioCorreoService {

    private static final Logger log = LoggerFactory.getLogger(ConsolaEnvioCorreoService.class);

    @Override
    public void enviarCodigoRecuperacion(String correo, String nombre, String codigo, int minutosVigencia) {
        log.info("[RECUPERACION] Para {} ({}): codigo {} (vigente {} minutos)", correo, nombre, codigo, minutosVigencia);
    }
}