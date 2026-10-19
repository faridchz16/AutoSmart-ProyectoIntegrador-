import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Fechas bloqueadas del mecánico (T22.2) – backend de Farid (T22.1, rama main):
     GET    /mechanics/me/blocked-dates?desde=AAAA-MM-DD  -> [{ idBloqueo, fecha, motivo }]  (por defecto desde hoy)
     POST   /mechanics/me/blocked-dates  { fecha, motivo? }                                  (motivo: máx. 200)
     DELETE /mechanics/me/blocked-dates/{id}
   El backend rechaza: fechas pasadas (400), fechas ya bloqueadas (409)
   y fechas con citas confirmadas (409). Las fechas se calculan en hora de Lima. */

const RUTA = "/mechanics/me/blocked-dates";

function normalizar(b = {}) {
  return { id: b.idBloqueo ?? b.id, fecha: String(b.fecha ?? "").slice(0, 10), motivo: b.motivo ?? "" };
}

export async function listarBloqueos(desde) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.listarBloqueos(desde)
    : await apiFetch(desde ? `${RUTA}?desde=${desde}` : RUTA);
  return (Array.isArray(datos) ? datos : []).map(normalizar).sort((a, b) => a.fecha.localeCompare(b.fecha));
}

export async function bloquearFecha(fecha, motivo) {
  const cuerpo = { fecha, motivo: motivo.trim() || null };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.bloquearFecha(cuerpo)
    : await apiFetch(RUTA, { metodo: "POST", cuerpo });
  return normalizar({ fecha, motivo: cuerpo.motivo, ...datos });
}

export async function desbloquearFecha(id) {
  if (prueba.DATOS_PRUEBA) return prueba.desbloquearFecha(id);
  await apiFetch(`${RUTA}/${id}`, { metodo: "DELETE" });
}

/* ---------- Utilidades de fechas (formato AAAA-MM-DD, hora de Lima) ---------- */

const dos = (n) => String(n).padStart(2, "0");

// Hoy en Lima, como "AAAA-MM-DD"
export function hoyLima() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "America/Lima" }).format(new Date());
}

export const isoDe = (anio, mes, dia) => `${anio}-${dos(mes + 1)}-${dos(dia)}`;

export function aFecha(iso) {
  const [a, m, d] = iso.split("-").map(Number);
  return new Date(a, m - 1, d);
}

// "martes, 21 de octubre de 2026"
export const fechaLarga = (iso) =>
  aFecha(iso).toLocaleDateString("es-PE", { weekday: "long", day: "numeric", month: "long", year: "numeric" });

// "mar 21 oct"
export const fechaCorta = (iso) =>
  aFecha(iso).toLocaleDateString("es-PE", { weekday: "short", day: "numeric", month: "short" });

// 1 = lunes … 7 = domingo (igual que diaSemana de los horarios)
export const diaSemanaDe = (iso) => aFecha(iso).getDay() || 7;
