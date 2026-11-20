package com.tecsup.autosmart.pasarela;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class YapePlinSandboxPasarela implements PasarelaPago {

    private static final String CODIGO_APROBADO = "123456";

    @Override
    public String metodo() {
        return "YAPE_PLIN";
    }

    @Override
    public ResultadoPasarela cobrar(SolicitudCobro solicitud) {
        String referencia = "YAPE-SANDBOX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        if (CODIGO_APROBADO.equals(solicitud.tokenPago())) {
            return ResultadoPasarela.aprobado(referencia);
        }
        return ResultadoPasarela.rechazado(referencia, "Codigo de aprobacion invalido");
    }
}