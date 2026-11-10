package com.tecsup.autosmart.event;

public record OrdenFinalizadaEvent(Long idOrden, Long idCita, String ticket, String placa,
                                   String servicio, String tokenCliente) {
}