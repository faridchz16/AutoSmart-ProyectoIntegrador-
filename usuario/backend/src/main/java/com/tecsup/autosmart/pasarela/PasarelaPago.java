package com.tecsup.autosmart.pasarela;

public interface PasarelaPago {

    String metodo();

    ResultadoPasarela cobrar(SolicitudCobro solicitud);
}