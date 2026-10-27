package com.tecsup.autosmart.event;

import java.math.BigDecimal;

// Datos de una cita recien reservada, listos para armar el correo
public record CitaReservadaEvent(
        String correoCliente,
        String nombreCliente,
        String ticket,
        String servicio,
        String mecanico,
        String placa,
        String fecha,
        String horaInicio,
        String horaFin,
        BigDecimal precio) {
}