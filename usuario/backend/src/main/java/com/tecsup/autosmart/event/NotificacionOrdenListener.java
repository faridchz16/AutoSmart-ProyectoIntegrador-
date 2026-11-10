package com.tecsup.autosmart.event;

import com.tecsup.autosmart.service.NotificacionPushService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class NotificacionOrdenListener {

    private final NotificacionPushService pushService;

    public NotificacionOrdenListener(NotificacionPushService pushService) {
        this.pushService = pushService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alFinalizarOrden(OrdenFinalizadaEvent evento) {
        pushService.enviar(
                evento.tokenCliente(),
                "Tu vehículo está listo",
                "El servicio " + evento.servicio() + " de tu vehículo " + evento.placa()
                        + " terminó. Ya puedes recogerlo.",
                Map.of(
                        "tipo", "ORDEN_FINALIZADA",
                        "idOrden", String.valueOf(evento.idOrden()),
                        "idCita", String.valueOf(evento.idCita()),
                        "ticket", evento.ticket()));
    }
}