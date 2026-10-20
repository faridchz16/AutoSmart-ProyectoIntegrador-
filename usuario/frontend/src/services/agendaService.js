import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Agenda del mecánico (T23.2 y T34.1) – backend de Farid (T23.1, rama main):
     GET /mechanics/me/appointments?desde=AAAA-MM-DD&hasta=AAAA-MM-DD
       -> [{ idCita, ticket, fecha, horaInicio, horaFin, estado, servicio, cliente, placa }]
   Sin parámetros devuelve las citas de hoy. El rango máximo es de 31 días.
   Estados: PENDIENTE, CONFIRMADA, RECHAZADA, CANCELADA. */

const RUTA = "/mechanics/me/appointments";

export const ESTADOS_CITA = {
  PENDIENTE: "Pendiente",
  CONFIRMADA: "Confirmada",
  RECHAZADA: "Rechazada",
  CANCELADA: "Cancelada",
};

const hora = (h) => String(h ?? "").slice(0, 5);

function normalizar(c = {}) {
  return {
    id: c.idCita ?? c.id,
    ticket: c.ticket ?? "",
    fecha: String(c.fecha ?? "").slice(0, 10),
    horaInicio: hora(c.horaInicio),
    horaFin: hora(c.horaFin),
    estado: String(c.estado ?? "PENDIENTE").toUpperCase(),
    servicio: c.servicio ?? "",
    cliente: c.cliente ?? "",
    placa: c.placa ?? "",
  };
}

const ordenar = (datos) =>
  (Array.isArray(datos) ? datos : [])
    .map(normalizar)
    .sort((a, b) => a.fecha.localeCompare(b.fecha) || a.horaInicio.localeCompare(b.horaInicio));

export async function listarAgenda(desde, hasta) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.listarAgenda(desde, hasta)
    : await apiFetch(`${RUTA}?desde=${desde}&hasta=${hasta}`);
  return ordenar(datos);
}

// Citas de hoy (T34.1): sin parámetros, el backend usa la fecha de hoy en Lima
export async function listarCitasHoy(hoy) {
  const datos = prueba.DATOS_PRUEBA ? await prueba.listarAgenda(hoy, hoy) : await apiFetch(RUTA);
  return ordenar(datos);
}
