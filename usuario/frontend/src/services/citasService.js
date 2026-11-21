import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Citas vistas por el mecánico – backend de Farid (rama main):
     GET /appointments/{id}  -> detalle (T35.1). Solo el mecánico asignado (403 si no; 404 si no existe)
       { idCita, ticket, fecha, horaInicio, horaFin, estado, precio, servicio, duracionMinutos,
         cliente, telefonoCliente, placa, marca, modelo, anio, kilometraje }
     PUT /appointments/{id}/accept                       -> confirma una cita PENDIENTE (T36.1)
     PUT /appointments/{id}/reject  { motivo }           -> la rechaza; motivo obligatorio, máx. 300 (T36.1)
   Si la cita ya no está pendiente (por ejemplo, el cliente la canceló), el backend responde 409.
     PUT /appointments/{id}/start  -> inicia la atención (T41.1): solo citas CONFIRMADA del mecánico asignado.
       Crea la orden de trabajo y la cita pasa a EN_PROCESO con la hora de inicio automática.
       -> { idOrden, idCita, ticket, estado, fechaInicio "AAAA-MM-DDTHH:mm:ss", servicio, placa }
       409 si la cita no está CONFIRMADA o si la atención ya fue iniciada. */

export const MAX_MOTIVO_RECHAZO = 300;

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

// Devuelven el nuevo estado de la cita
export async function aceptarCita(id) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.responderCita(id, "CONFIRMADA")
    : await apiFetch(`/appointments/${id}/accept`, { metodo: "PUT" });
  return String(datos?.estado ?? "CONFIRMADA").toUpperCase();
}

export async function rechazarCita(id, motivo) {
  const cuerpo = { motivo: motivo.trim() };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.responderCita(id, "RECHAZADA", cuerpo.motivo)
    : await apiFetch(`/appointments/${id}/reject`, { metodo: "PUT", cuerpo });
  return String(datos?.estado ?? "RECHAZADA").toUpperCase();
}

// Inicia la atención (T41.2). Devuelve la orden de trabajo creada.
export async function iniciarAtencion(id) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.iniciarAtencion(id)
    : await apiFetch(`/appointments/${id}/start`, { metodo: "PUT" });
  return {
    idOrden: datos?.idOrden ?? null,
    estado: String(datos?.estado ?? "EN_PROCESO").toUpperCase(),
    fechaInicio: datos?.fechaInicio ?? "",
  };
}

// "2026-11-03T09:05:00" -> "09:05"
export const horaDe = (fechaHora) => String(fechaHora ?? "").slice(11, 16);

// Texto del aviso que se muestra después de aceptar, rechazar o iniciar la atención
export function mensajeRespuesta(cita, estado, extra = {}) {
  if (estado === "CONFIRMADA") return `Confirmaste la cita de ${cita.cliente} (${cita.horaInicio}).`;
  if (estado === "EN_PROCESO") {
    const hora = horaDe(extra.fechaInicio);
    return `Iniciaste la atención de ${cita.cliente}${cita.placa ? ` (${cita.placa})` : ""}${hora ? ` a las ${hora}` : ""}.`;
  }
  return `Rechazaste la cita de ${cita.cliente}. Verá el motivo en la app.`;
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
