import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Reserva de citas del cliente (panel web). Backend de Farid (rama main):
     GET /services[?q=&categoria=PREVENTIVO|CORRECTIVO] -> servicios activos
          [{ idServicio, nombre, descripcion, precioBase, duracionMinutos, categoria, especialidad }]
     GET /services/{id}/mechanics -> mecánicos activos con la especialidad del servicio, de mejor a peor calificado
          [{ idMecanico, nombre, aniosExperiencia, especialidades[], calificacionPromedio (null sin reseñas), totalResenas }]
     GET /services/{id}/slots?mecanico=&fecha=AAAA-MM-DD -> turnos libres [{ horaInicio, horaFin }]
          Vacío si el taller cierra ese día, es feriado o el mecánico no atiende. 400 si la fecha ya pasó.
          OJO: el turno que el propio cliente tiene bloqueado NO aparece en esta lista.
     POST   /appointments/holds { idServicio, idMecanico, fecha, horaInicio } -> 201
          { idBloqueo, fecha, horaInicio, horaFin, expiraEn, segundosRestantes } (10 minutos).
          Un solo bloqueo por cliente: el nuevo reemplaza al anterior. 409 "El turno no esta disponible".
     DELETE /appointments/holds/{id} -> 204
     POST   /appointments { idServicio, idMecanico, idVehiculo, fecha, horaInicio } -> 201
          { idCita, ticket, estado: PENDIENTE, fecha, horaInicio, horaFin, precio, servicio, mecanico, placa }
          409 "El turno ya no esta disponible". Libera el bloqueo y envía el detalle al correo del cliente.
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

export const CATEGORIAS_SERVICIO = { PREVENTIVO: "Preventivo", CORRECTIVO: "Correctivo" };
export const DIAS_RESERVA = 21; // días que se muestran para elegir fecha (desde hoy)

const hora = (h) => String(h ?? "").slice(0, 5);
const numero = (n) => (n === null || n === undefined || n === "" ? null : Number(n));

const normalizarServicio = (s = {}) => ({
  id: s.idServicio ?? s.id,
  nombre: s.nombre ?? "",
  descripcion: s.descripcion ?? "",
  precio: Number(s.precioBase ?? s.precio ?? 0),
  duracion: Number(s.duracionMinutos ?? 0),
  categoria: CATEGORIAS_SERVICIO[String(s.categoria ?? "").toUpperCase()] ? String(s.categoria).toUpperCase() : "PREVENTIVO",
  especialidad: s.especialidad ?? "",
});

const normalizarMecanico = (m = {}) => ({
  id: m.idMecanico ?? m.id,
  nombre: m.nombre ?? "",
  experiencia: numero(m.aniosExperiencia),
  especialidades: Array.isArray(m.especialidades) ? m.especialidades : [],
  promedio: numero(m.calificacionPromedio),
  resenas: Number(m.totalResenas ?? 0),
});

const normalizarTurno = (t = {}) => ({ inicio: hora(t.horaInicio), fin: hora(t.horaFin) });

export async function listarCatalogo() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.catalogoReserva() : await apiFetch("/services");
  return (Array.isArray(datos) ? datos : []).map(normalizarServicio);
}

export async function listarMecanicos(idServicio) {
  const datos = prueba.DATOS_PRUEBA ? await prueba.mecanicosReserva(idServicio) : await apiFetch(`/services/${idServicio}/mechanics`);
  return (Array.isArray(datos) ? datos : []).map(normalizarMecanico);
}

export async function listarTurnos(idServicio, idMecanico, fecha) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.turnosReserva(idServicio, idMecanico, fecha)
    : await apiFetch(`/services/${idServicio}/slots?mecanico=${idMecanico}&fecha=${fecha}`);
  return (Array.isArray(datos) ? datos : []).map(normalizarTurno).sort((a, b) => a.inicio.localeCompare(b.inicio));
}

// Bloquea el turno 10 minutos mientras el cliente termina la reserva. expira = marca de tiempo local.
export async function bloquearTurno({ idServicio, idMecanico, fecha, horaInicio }) {
  const cuerpo = { idServicio, idMecanico, fecha, horaInicio };
  const b = prueba.DATOS_PRUEBA ? await prueba.bloquearTurnoReserva(cuerpo) : await apiFetch("/appointments/holds", { metodo: "POST", cuerpo });
  return {
    id: b.idBloqueo,
    fecha: String(b.fecha ?? fecha).slice(0, 10),
    inicio: hora(b.horaInicio ?? horaInicio),
    fin: hora(b.horaFin),
    idMecanico,
    expira: Date.now() + Math.max(0, Number(b.segundosRestantes ?? 600)) * 1000,
  };
}

// Libera el bloqueo sin interrumpir al usuario si falla (igual vence solo a los 10 minutos).
// keepalive permite enviarlo aunque se esté cerrando la pestaña.
export function liberarTurno(idBloqueo) {
  if (!idBloqueo) return Promise.resolve();
  const accion = prueba.DATOS_PRUEBA
    ? prueba.liberarTurnoReserva(idBloqueo)
    : apiFetch(`/appointments/holds/${idBloqueo}`, { metodo: "DELETE", keepalive: true });
  return accion.catch(() => {});
}

export async function reservarCita({ idServicio, idMecanico, idVehiculo, fecha, horaInicio }) {
  const cuerpo = { idServicio, idMecanico, idVehiculo, fecha, horaInicio };
  const c = prueba.DATOS_PRUEBA ? await prueba.reservarCitaCliente(cuerpo) : await apiFetch("/appointments", { metodo: "POST", cuerpo });
  return {
    id: c.idCita,
    ticket: c.ticket ?? "",
    estado: String(c.estado ?? "PENDIENTE").toUpperCase(),
    fecha: String(c.fecha ?? fecha).slice(0, 10),
    inicio: hora(c.horaInicio),
    fin: hora(c.horaFin),
    precio: numero(c.precio),
    servicio: c.servicio ?? "",
    mecanico: c.mecanico ?? "",
    placa: c.placa ?? "",
  };
}

/* ---------- Formatos ---------- */

const DIAS = ["domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"];
const MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];
const fechaUTC = (f) => new Date(`${f}T12:00:00Z`);

export const diaSemana = (f) => DIAS[fechaUTC(f).getUTCDay()];
export const esDomingo = (f) => fechaUTC(f).getUTCDay() === 0;
// "2026-11-24" -> "martes 24 de noviembre"
export const fechaLarga = (f) => (f ? `${diaSemana(f)} ${Number(f.slice(8, 10))} de ${MESES[Number(f.slice(5, 7)) - 1]}` : "");
export const mesCorto = (f) => MESES[Number(f.slice(5, 7)) - 1].slice(0, 3);

// "09:00" -> "9:00 a. m."
export function hora12(h) {
  if (!h) return "";
  const [hh, mm] = h.split(":").map(Number);
  return `${hh % 12 || 12}:${String(mm).padStart(2, "0")} ${hh < 12 ? "a. m." : "p. m."}`;
}
