package com.tecsup.autosmart.pasarela;

import com.tecsup.autosmart.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TarjetaSandboxPasarela implements PasarelaPago {

    @Override
    public String metodo() {
        return "TARJETA";
    }

    @Override
    public ResultadoPasarela cobrar(SolicitudCobro solicitud) {
        String referencia = "TARJETA-SANDBOX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return switch (solicitud.tokenPago()) {
            case "pm_card_visa", "pm_card_mastercard" -> ResultadoPasarela.aprobado(referencia);
            case "pm_card_chargeDeclined" -> ResultadoPasarela.rechazado(referencia, "La tarjeta fue rechazada");
            case "pm_card_chargeDeclinedInsufficientFunds" -> ResultadoPasarela.rechazado(referencia, "Fondos insuficientes");
            case "pm_card_chargeDeclinedExpiredCard" -> ResultadoPasarela.rechazado(referencia, "La tarjeta esta vencida");
            default -> throw new BadRequestException("El tokenPago no es valido para la pasarela");
        };
    }
}