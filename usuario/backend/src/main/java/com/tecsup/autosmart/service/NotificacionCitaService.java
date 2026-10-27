package com.tecsup.autosmart.service;

import com.tecsup.autosmart.event.CitaReservadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificacionCitaService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionCitaService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;

    public NotificacionCitaService(ObjectProvider<JavaMailSender> mailSender,
                                   @Value("${spring.mail.username:}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Async
    @TransactionalEventListener
    public void alReservar(CitaReservadaEvent e) {
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null || remitente == null || remitente.isBlank()) {
                log.info("[CORREO] No configurado: se omite la confirmacion de {} para {}",
                        e.ticket(), e.correoCliente());
                return;
            }

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(remitente);
            msg.setTo(e.correoCliente());
            msg.setSubject("AutoSmart - Cita registrada " + e.ticket());
            msg.setText("Hola " + e.nombreCliente() + ",\n\n"
                    + "Tu cita fue registrada y esta pendiente de confirmacion por el mecanico.\n\n"
                    + "Ticket: " + e.ticket() + "\n"
                    + "Servicio: " + e.servicio() + "\n"
                    + "Mecanico: " + e.mecanico() + "\n"
                    + "Vehiculo (placa): " + e.placa() + "\n"
                    + "Fecha: " + e.fecha() + "\n"
                    + "Horario: " + e.horaInicio() + " - " + e.horaFin() + "\n"
                    + "Precio: S/ " + e.precio() + "\n\n"
                    + "Gracias por confiar en AutoSmart.");
            sender.send(msg);
            log.info("[CORREO] Confirmacion {} enviada a {}", e.ticket(), e.correoCliente());
        } catch (Exception ex) {
            log.warn("[CORREO] No se pudo enviar la confirmacion {} a {}: {}",
                    e.ticket(), e.correoCliente(), ex.getMessage());
        }
    }
}