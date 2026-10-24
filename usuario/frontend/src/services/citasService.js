import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Citas vistas por el mecánico – backend de Farid (rama main):
     GET /appointments/{id}  -> detalle (T35.1). Solo el mecánico asignado (403 si no; 404 si no existe)
       { idCita, ticket, fecha, horaInicio, horaFin, estado, precio, servicio, duracionMinutos,
         cliente, telefonoCliente, placa, marca, modelo, anio, kilometraje } */

const hora = (h) => String(h ?? "").slice(0, 5);
const numero = (n) => (n === null || n === undefined || n === "" ? null : Number(n));

function normalizarDetalle(c = {}) {
  return {
    id: c.idCita ?? c.id,
    ticket: c.ticket ?? "",
    fecha: String(c.fecha ?? "").slice(0, 10),
    horaInicio: hora(c.horaInicio),
    horaFin: hora(c.horaFin),
    estado: String(c.estado ?? "PENDIENTE").toUpperCase(),
    precio: numero(c.precio),
    servicio: c.servicio ?? "",
    duracionMinutos: numero(c.duracionMinutos),
    cliente: c.cliente ?? "",
    telefono: c.telefonoCliente ?? c.telefono ?? "",
    placa: c.placa ?? "",
    marca: c.marca ?? "",
    modelo: c.modelo ?? "",
    anio: numero(c.anio),
    kilometraje: numero(c.kilometraje),
  };
}

export async function obtenerCita(id) {
  const datos = prueba.DATOS_PRUEBA ? await prueba.obtenerCita(id) : await apiFetch(`/appointments/${id}`);
  return normalizarDetalle(datos);
}

// Enlace de WhatsApp para un celular peruano (9 dígitos) o un número que ya trae el 51
export function enlaceWhatsApp(telefono) {
  const digitos = String(telefono ?? "").replace(/\D/g, "");
  if (/^9\d{8}$/.test(digitos)) return `https://wa.me/51${digitos}`;
  if (/^519\d{8}$/.test(digitos)) return `https://wa.me/${digitos}`;
  return null;
}

const MILES = new Intl.NumberFormat("es-PE");
export const formatoKm = (km) => (km === null ? "—" : `${MILES.format(km)} km`);
