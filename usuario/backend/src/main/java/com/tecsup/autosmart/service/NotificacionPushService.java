package com.tecsup.autosmart.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificacionPushService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionPushService.class);

    private final FcmTokenService fcmTokenService;

    public NotificacionPushService(FcmTokenService fcmTokenService) {
        this.fcmTokenService = fcmTokenService;
    }

    public boolean enviar(String token, String titulo, String cuerpo, Map<String, String> datos) {
        if (token == null || token.isBlank()) {
            log.info("Push no enviado: el usuario no tiene token FCM registrado");
            return false;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Push no enviado: Firebase no esta configurado");
            return false;
        }
        try {
            Message mensaje = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(titulo)
                            .setBody(cuerpo)
                            .build())
                    .putAllData(datos == null ? Map.of() : datos)
                    .build();
            String id = FirebaseMessaging.getInstance().send(mensaje);
            log.info("Push enviado: {}", id);
            return true;
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode codigo = e.getMessagingErrorCode();
            if (codigo == MessagingErrorCode.UNREGISTERED || codigo == MessagingErrorCode.INVALID_ARGUMENT) {
                log.warn("Token FCM invalido ({}), se elimina del usuario", codigo);
                fcmTokenService.invalidar(token);
            } else {
                log.warn("No se pudo enviar el push: {}", e.getMessage());
            }
            return false;
        } catch (Exception e) {
            log.warn("Error inesperado al enviar el push", e);
            return false;
        }
    }
}