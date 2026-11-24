import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";
import { listarCatalogo, listarMecanicos } from "./reservaService";
import { hoyLima } from "../utils/fechas";

/* Citas del cliente (panel web). Backend de Farid (rama main):
     GET /appointments/mine[?futuras=true] -> [{ idCita, ticket, estado, fecha, horaInicio, horaFin, precio, servicio, mecanico, placa }]
          de la más antigua a la más nueva. futuras=true: solo PENDIENTE y CONFIRMADA desde hoy.
     PUT /appointments/{id}/cancel -> la cita CANCELADA. Solo PENDIENTE o CONFIRMADA (409 si no) y con al menos
          2 horas de anticipación (400).
     PUT /appointments/{id}/reschedule { fecha, horaInicio } -> la cita en el nuevo turno, otra vez PENDIENTE
          (el mecánico debe confirmarla de nuevo). Solo PENDIENTE o CONFIRMADA y con al menos 24 horas de anticipación.
          409 "El nuevo turno no esta disponible" / "La cita ya esta en ese turno". Mismo servicio y mecánico.
   La respuesta no trae los ids del servicio ni del mecánico; para mostrar los turnos libres al reprogramar
   se buscan por nombre en el catálogo (GET /services y /services/{id}/mechanics).
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

export const ESTADOS_CITA_CLIENTE = {
  PENDIENTE: { texto: "Por confirmar", detalle: "El mecánico revisará tu reserva y la confirmará." },
  CONFIRMADA: { texto: "Confirmada", detalle: "Te esperamos con tu vehículo a la hora indicada." },
  EN_PROCESO: { texto: "En atención", detalle: "Tu vehículo está siendo atendido en el taller." },
  ESPERA_APROBACION: { texto: "Presupuesto por aprobar", detalle: "El mecánico encontró algo más y espera que apruebes su presupuesto." },
  FINALIZADA: { texto: "Lista para pagar", detalle: "El servicio terminó. Puedes pagar y recoger tu vehículo." },
  PAGADO: { texto: "Pagada", detalle: "Servicio terminado y pagado." },
  CANCELADA: { texto: "Cancelada", detalle: "Cancelaste esta cita." },
  RECHAZADA: { texto: "No aceptada", detalle: "El mecánico no pudo atenderla en ese horario. Puedes reservar otra." },
};
export const ESTADOS_ACTIVOS = ["PENDIENTE", "CONFIRMADA", "EN_PROCESO", "ESPERA_APROBACION", "FINALIZADA"];
export const HORAS_CANCELAR = 2;
export const HORAS_REPROGRAMAR = 24;

const hora = (h) => String(h ?? "").slice(0, 5);

function normalizarCita(c = {}) {
  const estado = String(c.estado ?? "").toUpperCase();
  return {
    id: c.idCita ?? c.id,
    ticket: c.ticket ?? "",
    estado: ESTADOS_CITA_CLIENTE[estado] ? estado : "PENDIENTE",
    fecha: String(c.fecha ?? "").slice(0, 10),
    inicio: hora(c.horaInicio),
    fin: hora(c.horaFin),
    precio: c.precio === null || c.precio === undefined ? null : Number(c.precio),
    servicio: c.servicio ?? "",
    mecanico: c.mecanico ?? "",
    placa: c.placa ?? "",
  };
}

export async function listarMisCitas() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.listarMisCitas(false) : await apiFetch("/appointments/mine");
  return (Array.isArray(datos) ? datos : []).map(normalizarCita);
}

// Próxima cita por atender (PENDIENTE o CONFIRMADA) o null
export async function proximaCita() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.listarMisCitas(true) : await apiFetch("/appointments/mine?futuras=true");
  const lista = (Array.isArray(datos) ? datos : []).map(normalizarCita).filter((c) => minutosPara(c) > 0);
  return lista.sort((a, b) => `${a.fecha}${a.inicio}`.localeCompare(`${b.fecha}${b.inicio}`))[0] ?? null;
}

export async function cancelarCita(id) {
  const datos = prueba.DATOS_PRUEBA ? await prueba.cancelarMiCita(id) : await apiFetch(`/appointments/${id}/cancel`, { metodo: "PUT" });
  return normalizarCita(datos);
}

export async function reprogramarCita(id, { fecha, horaInicio }) {
  const cuerpo = { fecha, horaInicio };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.reprogramarMiCita(id, cuerpo)
    : await apiFetch(`/appointments/${id}/reschedule`, { metodo: "PUT", cuerpo });
  return normalizarCita(datos);
}

// Busca el servicio y el mecánico de la cita para listar sus turnos libres. null si no se encuentran.
export async function datosParaReprogramar(cita) {
  const igual = (a, b) => a.trim().toLowerCase() === b.trim().toLowerCase();
  const servicio = (await listarCatalogo()).find((s) => igual(s.nombre, cita.servicio));
  if (!servicio) return null;
  const mecanico = (await listarMecanicos(servicio.id)).find((m) => igual(m.nombre, cita.mecanico));
  return mecanico ? { servicio, mecanico } : null;
}

/* ---------- Reglas de tiempo (hora de Lima, UTC-5 todo el año) ---------- */

export const minutosPara = (c) => Math.floor((new Date(`${c.fecha}T${c.inicio}:00-05:00`).getTime() - Date.now()) / 60000);
const activa = (c) => c.estado === "PENDIENTE" || c.estado === "CONFIRMADA";
export const puedeCancelar = (c) => activa(c) && minutosPara(c) >= HORAS_CANCELAR * 60;
export const puedeReprogramar = (c) => activa(c) && minutosPara(c) >= HORAS_REPROGRAMAR * 60;
// Pendiente o confirmada cuya hora ya pasó y nadie la atendió
export const vencida = (c) => activa(c) && minutosPara(c) < -60;
export const esDeHoy = (c) => c.fecha === hoyLima();
